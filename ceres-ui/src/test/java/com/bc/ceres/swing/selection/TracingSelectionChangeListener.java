/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.selection;

public class TracingSelectionChangeListener implements SelectionChangeListener {
    public String trace = "";

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        trace += "sc(" + event.getSelection().getPresentationName() + ");";
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
        trace += "scc;";
    }
}
