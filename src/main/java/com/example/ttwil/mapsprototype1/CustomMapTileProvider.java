package com.example.ttwil.mapsprototype1;

import android.content.res.AssetManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.util.SparseArray;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class CustomMapTileProvider implements TileProvider {
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final SparseArray<Rect> TILE_ZOOMS = new SparseArray<Rect>() {{
        put(8,  new Rect(135,  180,  135,  181 ));
        put(9,  new Rect(270,  361,  271,  363 ));
        put(10, new Rect(541,  723,  543,  726 ));
        put(11, new Rect(1082, 1447, 1086, 1452));
        put(12, new Rect(2165, 2894, 2172, 2905));
        put(13, new Rect(4330, 5789, 4345, 5810));
        put(14, new Rect(8661, 11578, 8691, 11621));
    }};

    private FileInputStream mFileInput;

    // CONSTRUCTOR
    public CustomMapTileProvider() {
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        if (zoom >= 10 && zoom <= 18) {
            byte[] image = readTileImage(x, y, zoom);
            return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);
        } else {
            return NO_TILE;
        }
    }

    private byte[] readTileImage(int x, int y, int zoom) {
        FileInputStream in = null;
        ByteArrayOutputStream buffer = null;

        try {
            File file = new File(getTileFilename(x, y, zoom));

            in = new FileInputStream(file);
            buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[BUFFER_SIZE];

            while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (buffer != null) try { buffer.close(); } catch (Exception ignored) {}
        }
    }

    private String getTileFilename(int x, int y, int zoom) {
        String output = (String) ("storage/DE94-B005/Nokia/" + zoom + "/" + x + "/" + y + ".png");
        return output;
    }

    private boolean hasTile(int x, int y, int zoom) {
        Rect b = TILE_ZOOMS.get(zoom);
        return b != null && (b.left <= x && x <= b.right && b.top <= y && y <= b.bottom);
    }
}
