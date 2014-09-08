package org.bdeinsalyon.drivelist;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Ben faut bien le lancer.
 * Created by Philippe on 08/09/2014.
 */
public class Main {

    private static String CLIENT_ID = "522351386560-0bpk4smfia9gnis27lsvjh4dp5oeof8m.apps.googleusercontent.com";
    private static String CLIENT_SECRET = "HKZe9fnMuIhXG8-kDb-uGsxp";

    private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

    private static Drive service;

    public static final List<String> SCOPES = Arrays.asList(
            // Required to access and manipulate files.
            "https://www.googleapis.com/auth/drive.file",
            // Required to identify the user in our data store.
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile");

    public static void main(String[] args){
        try {
            run();
            if(args.length>0){
                switch (args[0]){
                    case "clean":
                        removeDoubleDirectory(service);
                        break;
                    case "display":
                        displayEntries();
                        break;
                }
            } else {
                displayEntries();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void run() throws IOException {
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
                .setAccessType("online")
                .setApprovalPrompt("auto").build();

        String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
        System.out.println("Please open the following URL in your browser then type the authorization code:");
        System.out.println("  " + url);
        openWebpage(new URL(url));
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String code = br.readLine();

        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);

        Scanner in = new Scanner(System.in);

        //Create a new authorized API client
        service = new Drive.Builder(httpTransport, jsonFactory, credential).build();

        Folder.service = service;
        Folder.list();



    }

    private static void removeDoubleDirectory(Drive service) throws IOException {
        Scanner in = new Scanner(System.in);
        for(File twin: Folder.getTwins()){
            displayEntry(service, twin);
            System.out.println("Choisissez un parent à supprimer (-1 pour ne rien faire) :");
            int code = in.nextInt();
            if(code!=-1){
                try{
                    removeFileFromFolder(service,twin.getParents().get(code).getId(),twin.getId());
                    System.out.println("Supprimé du dossier "+code);
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else
                System.out.println("On a rien fait pour celui là");
            System.out.println(' ');
        }
    }

    private static void displayEntries() throws IOException {
        for(File twin: Folder.getTwins()) {
            displayEntry(service, twin);
        }
    }

    private static void displayEntry(Drive service, File entry) throws IOException {
        System.out.println("Le dossier '"+ entry.getTitle()+"' (ID:"+ entry.getId()+") est présent dans deux dossiers : ");
        List<ParentReference> parents = entry.getParents();
        for (int i = 0; i < parents.size(); i++) {
            ParentReference parentReference = parents.get(i);
            File folder = service.files().get(parentReference.getId()).execute();
            System.out.println(" ["+i+"] "+folder.getTitle()+" (voir à https://drive.google.com/drive/u/0/?rfd=1#folders/"+parentReference.getId()+")");
        }
    }


    /**
     * Insert a file into a folder.
     *
     * @param service Drive API service instance.
     * @param folderId ID of the folder to insert the file into
     * @param fileId ID of the file to insert.
     * @return The inserted parent if successful, {@code null} otherwise.
     */
    private static ParentReference insertFileIntoFolder(Drive service, String folderId,
                                                        String fileId) {
        ParentReference newParent = new ParentReference();
        newParent.setId(folderId);
        try {
            return service.parents().insert(fileId, newParent).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
        return null;
    }

    /**
     * Remove a file from a folder.
     *
     * @param service Drive API service instance.
     * @param folderId ID of the folder to remove the file from.
     * @param fileId ID of the file to remove from the folder.
     */
    private static void removeFileFromFolder(Drive service, String folderId,
                                             String fileId) {
        try {
            service.parents().delete(fileId, folderId).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
    }

    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
