package org.bdeinsalyon.drivelist;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Philippe on 08/09/2014.
 */
public class Folder {

    public static Drive service;

    private static final ArrayList<File> twins = new ArrayList<File>();

    public static void list(){
        try {
            Drive.Files.List list = service.files().list();
            list.setQ("mimeType='application/vnd.google-apps.folder'");
            list.setMaxResults(1000);
            do {
                FileList execute = list.execute();
                for (File child : execute.getItems()) {
                    //if you look for twins folders
                    if (child.getParents().size() > 1 && !isTwin(child)) {
                        twins.add(child);
                    }
                    //if you look for files > n 
                    //cf drive.API
                }
                list.setPageToken(execute.getNextPageToken());
            } while (list.getPageToken() != null &&
                    list.getPageToken().length() > 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isTwin(File id) {
        for(File s:twins)
            if(s.getId().equals(id.getId()))
                return true;
        return false;
    }

    public static ArrayList<File> getTwins() {
        return twins;
    }
}
