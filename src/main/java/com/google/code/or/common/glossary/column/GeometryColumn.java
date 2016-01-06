package com.google.code.or.common.glossary.column;

import com.google.code.or.common.glossary.Column;
import com.vividsolutions.jts.geom.Geometry;

public class GeometryColumn implements Column {
    private Geometry geometry;

    public GeometryColumn(Geometry g) {
        this.geometry  = g;
    }

    public Geometry getValue() {
        return this.geometry;
    }

    public static GeometryColumn valueOf(Geometry g) {
        return new GeometryColumn(g);
    }
}

