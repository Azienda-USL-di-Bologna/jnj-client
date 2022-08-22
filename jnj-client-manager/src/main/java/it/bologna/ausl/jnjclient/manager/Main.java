package it.bologna.ausl.jnjclient.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.jnjclient.firmajnj.signer.SignToken;
import it.bologna.ausl.jnjclient.firmajnj.signer.Signer;
import it.bologna.ausl.jnjclient.firmajnj.signer.SignerFactory;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.FirmaJnJException;
import it.bologna.ausl.jnjclient.firmajnj.signer.exceptions.UserAbortException;
import it.bologna.ausl.jnjclient.firmajnj.tools.GlobalTools;
import it.bologna.ausl.jnjclient.firmajnj.utils.HttpUtils;
import it.bologna.ausl.jnjclient.library.JnJClientCommand;
import it.bologna.ausl.jnjclient.library.JnJProgressBar;
import it.bologna.ausl.jnjclient.library.exceptions.JnJClientCommandException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author gdm
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    public static final String PROTOCOL = "firmajnj:";
    public static final String RESUME_FILE_NAME = "resumeParams.txt";
    
    private static final ObjectMapper objectMapper = GlobalTools.getObjectMapper();
    
    public static void main(String[] args) throws JnJClientCommandException, ParseException, UnsupportedEncodingException, IOException {

            //        usage: jnj-client
//        -c,--command <arg>   command to execute: es. FIRMA_JNJ
//        -p,--params <arg>    command params: es. per la FIRMA_JNJ, l'url dal quale scaricare i SignParams. es. http://localhost:10005/firma-api/jnj/getParameters?token=cef78822-0c9c-4891-b66d-a1e494cceca9 (in base64): aHR0cDovL2xvY2FsaG9zdDoxMDAwNS9maXJtYS1hcGkvam5qL2dldFBhcmFtZXRlcnM/dG9rZW49Y2VmNzg4MjItMGM5Yy00ODkxLWI2NmQtYTFlNDk0Y2NlY2E5
//        -s,--server <arg>    serverUrl: es. http://localhost:10005/firma-api/jnj serve per auto-update
        
//        example call: <program> -c FIRMA_JNJ -p "aHR0cDovL2xvY2FsaG9zdDoxMDAwNS9maXJtYS1hcGkvam5qL2dldFBhcmFtZXRlcnM/dG9rZW49Y2VmNzg4MjItMGM5Yy00ODkxLWI2NmQtYTFlNDk0Y2NlY2E5"  -s "http://localhost:10005/firma-api/jnj" -e BASE64
//        example call: <program> -c FIRMA_JNJ -s http://localhost:10005/firma-api/jnj -p "http%3A%2F%2Flocalhost%3A10005%2Ffirma-api%2Fjnj%2FgetParameters%3Ftoken%3Dcef78822-0c9c-4891-b66d-a1e494cceca9" -e URL        

//      from browser: firmajnj:-c%20FIRMA_JNJ%20-s%20http://localhost:10005/firma-api/jnj%20-p%20%22http%3A%2F%2Flocalhost%3A10005%2Ffirma-api%2Fjnj%2FgetParameters%3Ftoken%3Dcef78822-0c9c-4891-b66d-a1e494cceca9%22
        JnJProgressBar progressBar = null;
        try {
            progressBar = new JnJProgressBar();
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                LOGGER.log(Level.WARNING, "error setting look and feel");
            }
            // create Options object
            Options options = new Options();
            options.addOption("h", "help", false, "print help (this screen)");
            options.addOption("r", "resume", false, "resume reading " + RESUME_FILE_NAME);
            options.addOption("s", "server", true, "check-update url");
            options.addOption("c", "command", true, "command to execute");

            Option option = new Option("p", "params", true, "command params");
            option.setArgs(Option.UNLIMITED_VALUES);
            options.addOption(option);

            options.addOption("e", "encoding", true, "command params enconding, possible values are BASE64, URL, NONE");

            // se lanciato da browser i comandi arrivano come una unica stringa url-encodati, nel caso creo i parametri come dovrebbero essere parsando la stringa
            if (args != null && args.length > 0 && args[0] != null && args[0].startsWith(PROTOCOL)) {
                String paramsDcoded = URLDecoder.decode(args[0], "UTF-8");
                String[] argsSplitted = paramsDcoded.substring(PROTOCOL.length()).split(" ");
                int i = 0;
                args = new String[argsSplitted.length];
                for (String arg : argsSplitted) {
                    args[i] = arg;
                    i++;
                }
            }

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            HelpFormatter formatter = new HelpFormatter();

            if (cmd.hasOption("r")) {
                File resumeFile = new File(RESUME_FILE_NAME);
                if (!resumeFile.exists()) {
                    LOGGER.log(Level.INFO, "nothing to resume, exiting...");
                    System.exit(0);
                }
                String resumeParams = Files.readString(resumeFile.toPath());
                try {
                    if (!resumeFile.delete()) {
                        resumeFile.deleteOnExit();
                        throw new FileSystemException("resumeParams not deleted");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "error deleting resumeParams");
                }
                args = resumeParams.split(",");
                cmd = parser.parse(options, args);
            }

            String checkUpdateUrl = cmd.getOptionValue("s");
            String command = cmd.getOptionValue("c");
            String[] commandParams = cmd.getOptionValues("p");

            String paramsEncoding = null;

            if (cmd.hasOption("e")) {
                paramsEncoding = cmd.getOptionValue("e");
                switch (paramsEncoding) {
                    case "BASE64": 
                        for (int i=0; i<commandParams.length; i++) {
                            commandParams[i] = new String(Base64.getDecoder().decode(commandParams[i]), "UTF-8");
                        }
                        break;
                    case "URL":
                        for (int i=0; i<commandParams.length; i++) {
                            commandParams[i] = URLDecoder.decode(commandParams[i], "UTF-8");
                        }
                        break;
                    case "NONE": 
                        break;
                    default:
                        System.out.println(String.format("encoding %s not valid", paramsEncoding));

                }
            }

            LOGGER.log(Level.INFO, "parametri: ");
            LOGGER.log(Level.INFO, checkUpdateUrl);
            LOGGER.log(Level.INFO, command);
            LOGGER.log(Level.INFO, paramsEncoding);
            LOGGER.log(Level.INFO, Arrays.toString(commandParams));

            if (cmd.hasOption("h")) {
                formatter.printHelp("jnj-client", options);
                return;
            }

            progressBar.setVisible(true);
            progressBar.addStep(5);
            checkUpdate(args, checkUpdateUrl);
            progressBar.addStep(5);
    //        JnJCommandFactory.getCommand("FIRMA_JNJ");
            JnJClientCommand jClientCommand = JnJCommandFactory.getCommand(command);
            SignParamsComponent.EndSign.EndSignResults res = jClientCommand.executeCommand(progressBar, commandParams);
            progressBar.setTitle("Terminazione...");
            progressBar.setValue(progressBar.getMaxValue());
            try {
                System.out.println(new ObjectMapper().writeValueAsString(res));
            } catch (JsonProcessingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Throwable t) {
            if (progressBar != null) {
                progressBar.terminate();
            }
            throw t;
        }
        progressBar.terminate();
    }
    
    public static void checkUpdate(String[] args, String checkUpdateUrl) {
        boolean newVersion = false;
        String msiFileName = null;
        LOGGER.log(Level.INFO, "checking new version... ");
        try {
            String url = "[checkUpdateUrl]?version=[version]";
            Map<String, String> clientInfo = objectMapper.readValue(new File("client-info.json"), new TypeReference<Map<String, String>>() {});
            String version = clientInfo.get("version");
            msiFileName = clientInfo.get("msi-file");
            LOGGER.log(Level.INFO, String.format("version: %s", version));
            LOGGER.log(Level.INFO, String.format("msiFileName: %s", msiFileName));
            url = url.replace("[checkUpdateUrl]", checkUpdateUrl).replace("[version]", version);
            LOGGER.log(Level.INFO, String.format("url: %s", url));
            OkHttpClient httpClient = HttpUtils.getHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            LOGGER.log(Level.INFO, "executing request...");
            Response resp = httpClient.newCall(request).execute();
            LOGGER.log(Level.INFO, String.format("response code: %s", resp.code()));
            if (resp.isSuccessful()) {
                if (resp.code() == 201) {
                    LOGGER.log(Level.INFO, "new version found, downloading...");
                    File updatePathDir = new File("update");
                    if (!updatePathDir.exists()) {
                        updatePathDir.mkdir();
                    }
                    long copy = Files.copy(resp.body().byteStream(), new File(updatePathDir, msiFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.log(Level.INFO, "new version downloaded");
                    newVersion = true;
                } else {
                    LOGGER.log(Level.INFO, "client already updated");
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "error checking new version...", ex);
        }
        if (newVersion) {
            LOGGER.log(Level.INFO, "update in progess...");
            launchUpdate(args, msiFileName);
        }
    }
    
    public static void launchUpdate(String[] args, String msiFileName) {
         LOGGER.log(Level.INFO, "dumping call parameters...");
        String str = String.join(",", args);
        try (FileWriter fw = new FileWriter(RESUME_FILE_NAME)) {
            fw.write(str);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "error dumping call parameters", ex);
        }
        
        try {
            LOGGER.log(Level.INFO, "launching setup...");
            Runtime rf = Runtime.getRuntime();  
            Process process = rf.exec(String.format("msiexec /i \"update\\%s\"", msiFileName));
            LOGGER.log(Level.INFO, "setup launched, exiting application...");
            System.exit(0);
        } catch(Throwable t) {                 
            LOGGER.log(Level.SEVERE, "error launching setup", t);
        }
    }
    
    public static void test(String[] args) throws FirmaJnJException, IOException {
        Signer signer = SignerFactory.getSigner(SignerFactory.PKCSStandards.PKCS_11, false);
        try {
            SignToken signToken = signer.getSignToken();

            SignParamsComponent.SignFileAttributes sfa = new SignParamsComponent.SignFileAttributes();
            SignParamsComponent.SignFileAttributesPosition signFileAttributesPosition = new SignParamsComponent.SignFileAttributesPosition();
            signFileAttributesPosition.setFieldOriginX(10);
            signFileAttributesPosition.setFieldOriginY(10);
            signFileAttributesPosition.setFieldWidth(200);
            signFileAttributesPosition.setFieldHeight(100);
            signFileAttributesPosition.setAlignmentHorizontal(SignParamsComponent.SignFileAttributesPosition.AlignmentHorizontalPositions.LEFT);
            signFileAttributesPosition.setAlignmentVertical(SignParamsComponent.SignFileAttributesPosition.AlignmentVerticalPositions.TOP);
            sfa.setPosition(signFileAttributesPosition);
            sfa.setVisible(true);
            sfa.setTextTemplate("firmato da [COMMONNAME] per azienda  [ORGANIZATIONNAME]....[GIVENNAME]...[SURNAME]...[SERIALNUMBER]..[COUNTRYNAME]..[DNQUALIFIER]");
            DSSDocument Psign = signer.padesSign(signToken, new FileDocument("test-pdf.pdf"), sfa);
            Psign.save("test-pdf-signed.pdf");
            DSSDocument Csign = signer.cadesSign(signToken, new FileDocument("test-pdf.pdf"));
            Csign.save("test-pdf-signed.pdf.p7m");
        } catch (UserAbortException ex) {
            System.out.println("Annullato dall'utente");
        } catch (Exception ex) {
            System.out.println("errore");
            ex.printStackTrace();
        }
    }
}
