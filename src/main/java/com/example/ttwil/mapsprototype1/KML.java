package com.example.ttwil.mapsprototype1;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KML {

    @SuppressLint("DefaultLocale")
    public void createKML(List<Marker> markers, int session) throws IOException {
        StringBuilder kmlStringBuilder = new StringBuilder(500 * (markers.size() + 3));

        // add KML header
        kmlStringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kmlStringBuilder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kmlStringBuilder.append("<Document>\n");
        kmlStringBuilder.append("<name>Document.kml</name>\n");

        // add style for marker
        kmlStringBuilder.append("<Style id=\"blueMarker\">\n");
        kmlStringBuilder.append("<LabelStyle>\n");
        kmlStringBuilder.append("<color>ff00ff00</color>\n");
        kmlStringBuilder.append("</LabelStyle>\n");
        kmlStringBuilder.append("</Style>\n");

        // add markers as KML placemarks
        for (Marker marker : markers) {
            kmlStringBuilder.append("<Placemark>\n");
            // kmlStringBuilder.append(String.format("<name>%s</name>\n", marker.getTitle()));
            // kmlStringBuilder.append("<styleUrl>#blueMarker</styleUrl>\n");
            kmlStringBuilder.append("<Style>\n");
            kmlStringBuilder.append("<IconStyle>\n");
            kmlStringBuilder.append("<heading>");
            // kmlStringBuilder.append(marker.getRotation());
            kmlStringBuilder.append("160");
            kmlStringBuilder.append("</heading>\n");
            kmlStringBuilder.append("<Icon>");
            if (marker.getTitle().equals("1")) {
                kmlStringBuilder.append("<href>http://worldartsme.com/images/green-dot-clipart-1.jpg</href>");
            }
            else {
                kmlStringBuilder.append("<href>https://www.marylandeyeassociates.com/wp-content/uploads/2015/03/red-dot-hi.png</href>");
            }
            kmlStringBuilder.append("</Icon>\n");
            kmlStringBuilder.append("<scale>0.5</scale>\n");
            kmlStringBuilder.append("</IconStyle>\n");
            kmlStringBuilder.append("</Style>\n");
            kmlStringBuilder.append("<Point>\n");
            kmlStringBuilder.append(String.format("<coordinates>%8.6f,%8.6f,0</coordinates>\n",
                    marker.getPosition().longitude, marker.getPosition().latitude));
            kmlStringBuilder.append("</Point>\n");
            kmlStringBuilder.append("</Placemark>\n");
        }

        // add end tags
        kmlStringBuilder.append("</Document>\n");
        kmlStringBuilder.append("</kml>\n");

        File kmlFolder = new File(Environment.getExternalStorageDirectory(), "KML");
        if (!kmlFolder.exists()) {
            if (!kmlFolder.mkdirs()) { return; }
        }

        File kmlFile = new File(kmlFolder, "Session" + session + ".kml");

        FileOutputStream fileOutputStream = new FileOutputStream(kmlFile);
        fileOutputStream.write(kmlStringBuilder.toString().getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

//    public static String exportMarkersToKML(List<Marker> markers) {
//        StringBuilder kmlStringBuilder = new StringBuilder(500 * (markers.size() + 3));
//
//        // add KML header
//        kmlStringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//        kmlStringBuilder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
//        kmlStringBuilder.append("<Document>\n");
//        kmlStringBuilder.append("<name>Document.kml</name>\n");
//
//        // add style for marker
//        kmlStringBuilder.append("<Style id=\"blueMarker\">\n");
//        kmlStringBuilder.append("<LabelStyle>\n");
//        kmlStringBuilder.append("<color>ff00ff00</color>\n");
//        kmlStringBuilder.append("</LabelStyle>\n");
//        kmlStringBuilder.append("</Style>\n");
//
//        // add markers as KML placemarks
//        for (Marker marker : markers) {
//            kmlStringBuilder.append("<Placemark>\n");
//            kmlStringBuilder.append(String.format("<name>%s</name>\n", marker.getTitle()));
//            kmlStringBuilder.append("<styleUrl>#blueMarker</styleUrl>\n");
//            kmlStringBuilder.append("<Point>\n");
//            kmlStringBuilder.append(String.format("<coordinates>%8.3f,%8.3f,0</coordinates>\n",
//                    marker.getPosition().longitude, marker.getPosition().latitude));
//            kmlStringBuilder.append("</Point>\n");
//            kmlStringBuilder.append("</Placemark>\n");
//        }
//
//        kmlStringBuilder.append("</Document>\n");
//        kmlStringBuilder.append("</kml>\n");
//        return kmlStringBuilder.toString();
//    }
//
//    public static void exportMarkersToKMZfile(List<Marker> markers) {
//        try {
//
//            String kmlString = exportMarkersToKML(markers);
//
//            File kmzFolder = new File(Environment.getExternalStorageDirectory(), "KMZ");
//            if (!kmzFolder.exists()) {
//                kmzFolder.mkdir();
//            }
//
//            File kmzFile = new File(kmzFolder, "markers.kmz");
//
//            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(kmzFile));
//            ZipEntry entry = new ZipEntry("markers.kml");
//            zipOutputStream.putNextEntry(entry);
//            zipOutputStream.write(kmlString.getBytes());
//            zipOutputStream.flush();
//            zipOutputStream.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
