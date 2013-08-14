package org.jeo.geom;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;

public class GeometryCollectionPath extends CoordinatePath {

    GeometryCollection gcol;

    CoordinatePath it;
    int i;

    GeometryCollectionPath(GeometryCollection gcol) {
        this.gcol = gcol;
        doReset();
    }

    @Override
    public Geometry getGeometry() {
        return gcol;
    }

    @Override
    protected PathStep doNext(Coordinate c) {
        while(!it.hasNext()) {
            if (++i == gcol.getNumGeometries()) {
                return PathStep.STOP;
            }

            it = CoordinatePath.create(gcol.getGeometryN(i));
        }

        return next(c);
    }

    PathStep next(Coordinate c) {
        Coordinate d = it.next();
        c.x = d.x;
        c.y = d.y;

        return it.getStep();
    }

    @Override
    protected void doReset() {
        i = 0;
        it = CoordinatePath.create(gcol.getGeometryN(i));
    }

}
