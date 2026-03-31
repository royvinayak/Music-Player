package com.playlisto.model;

import java.util.ArrayList;
import java.util.List;

public class FolderData {
    private String lastFolder;
    private List<FolderEntry> folders = new ArrayList<>();

    public FolderData() {}

    public String getLastFolder() {
        return lastFolder;
    }

    public void setLastFolder(String lastFolder) {
        this.lastFolder = lastFolder;
    }

    public List<FolderEntry> getFolders() {
        return folders;
    }

    public void setFolders(List<FolderEntry> folders) {
        this.folders = folders;
    }

    public static class FolderEntry {
        private String name;
        private List<DriveFile> files = new ArrayList<>();

        public FolderEntry() {}

        public FolderEntry(String name, List<DriveFile> files) {
            this.name = name;
            this.files = files;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<DriveFile> getFiles() {
            return files;
        }

        public void setFiles(List<DriveFile> files) {
            this.files = files;
        }
    }
}
