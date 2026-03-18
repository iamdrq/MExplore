package com.nd.me.filesystem;

import java.util.List;

public class FileSorter {

    public static void sort(List<IFileItem> files, FileSortType type) {
        if (files == null || files.isEmpty()) return;

        files.sort((f1, f2) -> {
            // dir first
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;

            switch (type) {
                case NAME_ASC:
                    return f1.getName().compareToIgnoreCase(f2.getName());
                case NAME_DESC:
                    return f2.getName().compareToIgnoreCase(f1.getName());
                case SIZE_ASC:
                    return Long.compare(f1.getSize(), f2.getSize());
                case SIZE_DESC:
                    return Long.compare(f2.getSize(), f1.getSize());
                case DATE_ASC:
                    return Long.compare(f1.getModTime(), f2.getModTime());
                case DATE_DESC:
                    return Long.compare(f2.getModTime(), f1.getModTime());
            }
            return 0;
        });
    }
}
