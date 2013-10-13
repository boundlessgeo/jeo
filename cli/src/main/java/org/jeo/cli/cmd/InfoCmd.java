package org.jeo.cli.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.List;

import jline.console.ConsoleReader;

import org.jeo.cli.JeoCLI;
import org.jeo.data.DataRef;
import org.jeo.data.Dataset;
import org.jeo.data.DirectoryRegistry;
import org.jeo.data.Drivers;
import org.jeo.data.Query;
import org.jeo.data.TileGrid;
import org.jeo.data.TilePyramid;
import org.jeo.data.TileDataset;
import org.jeo.data.VectorDataset;
import org.jeo.data.Workspace;
import org.jeo.feature.Field;
import org.jeo.feature.Schema;
import org.jeo.proj.Proj;
import org.osgeo.proj4j.CoordinateReferenceSystem;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.vividsolutions.jts.geom.Envelope;

@Parameters(commandNames="info", commandDescription="Provides information about a data source")
public class InfoCmd extends JeoCmd {

    @Parameter(description="datasource", required=true)
    List<String> datas;

    @Override
    protected void doCommand(JeoCLI cli) throws Exception {
        for (String data : datas) {
            URI uri = parseDataURI(data);

            try {
                Object obj = Drivers.open(uri);
                if (obj == null) {
                    throw new IllegalArgumentException("Unable to open data source: " + uri);
                }
    
                print(obj, cli);
            }
            catch(Exception e) {
                File f = new File(uri);
                if (f.exists() && f.isDirectory()) {
                    DirectoryRegistry reg = new DirectoryRegistry(f);
                    try {
                        for (DataRef<?> it : reg.list()) {
                            print(reg.get(it.getName()), cli);
                        }
                    }
                    finally {
                        reg.close();
                    }
                }
                else {
                    throw e;
                }
            }
        }
    }

    void print(Object obj, JeoCLI cli) throws IOException {
        if (obj instanceof Workspace) {
            print((Workspace)obj, cli);
        }
        else if (obj instanceof VectorDataset) {
            print((VectorDataset)obj, cli);
        }
        else if (obj instanceof TileDataset) {
            print((TileDataset)obj, cli);
        }
        else {
            throw new IllegalArgumentException(
                "Object " + obj.getClass().getName() + " not supported");
        }
        cli.getConsole().println();
    }

    void print(Dataset dataset, JeoCLI cli) throws IOException {
        ConsoleReader console = cli.getConsole();
        console.println("Name:   " + dataset.getName());
        console.println("Driver: " + dataset.getDriver().getName());

        Envelope bbox = dataset.bounds();
        console.println("Bounds: " + String.format("%f, %f, %f, %f",
            bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY()));

        CoordinateReferenceSystem crs = dataset.crs();
        console.println("CRS:    " + (crs != null ? crs.getName() : "None"));
        if (crs != null) {
            print(crs, cli);
        }
    }

    void print(VectorDataset dataset, JeoCLI cli) throws IOException {
        ConsoleReader console = cli.getConsole();
        try {
            print((Dataset) dataset, cli);

            console.println("Count:  " + dataset.count(new Query()));
            console.println("Schema:");

            Schema schema = dataset.schema();
            int size = new Ordering<Field>() {
                public int compare(Field left, Field right) {
                    return Ints.compare(left.getName().length(), right.getName().length());
                };
            }.max(schema.getFields()).getName().length() + 2;

            for (Field fld : schema ) {
                console.print(Strings.padStart(fld.getName(), size, ' '));
                console.println(" : " + fld.getType().getSimpleName());
            }
        }
        finally {
            dataset.close();
        }
    }

    void print(TileDataset dataset, JeoCLI cli) throws IOException {
        ConsoleReader console = cli.getConsole();
        try {
            print((Dataset) dataset, cli);

            TilePyramid pyr = dataset.pyramid();

            console.println(
                String.format("Tilesize: %d, %d", pyr.getTileWidth(), pyr.getTileHeight()));
            console.println("Tilesets:");
            for (TileGrid grid : dataset.pyramid().getGrids()) {
                int width = grid.getWidth();
                int height = grid.getHeight();
                
                console.print("\t");
                console.println(String.format("%d: %d x %d (%d); %f, %f", grid.getZ(), 
                    width, height, width*height, grid.getXRes(), grid.getYRes()));
            }
        }
        finally {
            dataset.close();
        }
    }

    void print(Workspace workspace, JeoCLI cli) throws IOException {
        ConsoleReader console = cli.getConsole();
        try {
            console.println("Driver: " + workspace.getDriver().getName());
            console.println("Datasets:");

            for (DataRef<? extends Dataset> ref : workspace.list()) {
                console.print("\t");
                console.println(ref.getName());
            }
        }
        finally {
            workspace.close();
        }
    }

    void print(CoordinateReferenceSystem crs, JeoCLI cli) throws IOException {
        ConsoleReader console = cli.getConsole();

        String wkt = Proj.toWKT(crs, true);
        BufferedReader r = new BufferedReader(new StringReader(wkt));
        String line = null;
        while ((line = r.readLine()) != null) {
            console.print("\t\t");
            console.println(line);
        }
    }
}
