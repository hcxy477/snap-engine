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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PixelExtractionProcessingForm {

    private JPanel panel;
    private JLabel windowLabel;
    private JSpinner windowSpinner;
    private AppContext appContext;

    public PixelExtractionProcessingForm(AppContext appContext, PropertyContainer container) {

        this.appContext = appContext;

        final TableLayout tableLayout = new TableLayout(3);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellColspan(0, 0, 3);
        tableLayout.setCellColspan(1, 0, 3);
        tableLayout.setCellColspan(2, 0, 3);
        tableLayout.setCellFill(1, 1, TableLayout.Fill.BOTH);
        tableLayout.setCellWeightY(3, 1, 1.0);
        tableLayout.setCellFill(3, 1, TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(1, 1.0);

        panel = new JPanel(tableLayout);
        final BindingContext bindingContext = new BindingContext(container);

        panel.add(createIncludeCheckbox(bindingContext, "Export bands", "exportBands"));
        panel.add(createIncludeCheckbox(bindingContext, "Export tie-point grids", "exportTiePoints"));
        panel.add(createIncludeCheckbox(bindingContext, "Export masks", "exportMasks"));

        panel.add(new JLabel("Coordinates:"));
        final JComponent[] coordinatesComponents = createCoordinatesComponents(container);
        panel.add(coordinatesComponents[0]);
        panel.add(coordinatesComponents[1]);

        panel.add(new JLabel("Placemark file:"));
        final JComponent[] pinFileComponents = createPinFileComponents(bindingContext);
        panel.add(pinFileComponents[0]);
        panel.add(pinFileComponents[1]);

        panel.add(new JLabel("Window size:"));
        windowSpinner = createWindowSizeEditor(bindingContext);
        panel.add(windowSpinner);
        windowLabel = new JLabel();
        windowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        windowSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateWindowLabel();
            }
        });
        updateWindowLabel();
        panel.add(windowLabel);
    }

    private void updateWindowLabel() {
        windowLabel.setText(String.format("%1$d x %1$d", (Integer) windowSpinner.getValue()));
    }

    private JComponent[] createPinFileComponents(BindingContext bindingContext) {
        final JTextField textField = new JTextField();
        final ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind("coordinatesFile", adapter);
        final JButton ellipsesButton = new JButton("...");
        ellipsesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fileChooser = new JFileChooser();
                int i = fileChooser.showDialog(panel, "Select");
                if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                    binding.setPropertyValue(fileChooser.getSelectedFile());
                }
            }
        });
        return new JComponent[]{textField, ellipsesButton};
    }

    private JCheckBox createIncludeCheckbox(BindingContext bindingContext, String labelText, String propertyName) {
        final Property squareSizeProperty = bindingContext.getPropertySet().getProperty(propertyName);
        final Boolean defaultValue = (Boolean) squareSizeProperty.getDescriptor().getDefaultValue();
        final JCheckBox checkbox = new JCheckBox(labelText, defaultValue);
        bindingContext.bind(propertyName, checkbox);
        return checkbox;
    }

    private JComponent[] createCoordinatesComponents(PropertyContainer container) {
        final GenericListModel<GeoPos> listModel = new GenericListModel<GeoPos>(container.getProperty("coordinates"));
        final JList coordinateList = new JList(listModel);
        coordinateList.setVisibleRowCount(5);
        coordinateList.setCellRenderer(new GeoPosListCellRenderer());
        coordinateList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JScrollPane rasterScrollPane = new JScrollPane(coordinateList);
        rasterScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rasterScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GeoPosDialog dialog = createGeoPosDialog();
                if (dialog.show() != AbstractDialog.ID_OK) {
                    return;
                }
                Float lat = dialog.getLat();
                Float lon = dialog.getLon();
                try {
                    listModel.addElement(new GeoPos(lat, lon));
                } catch (ValidationException ignored) {
                }
            }
        });
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"),
                                                                           false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] selectedValues = coordinateList.getSelectedValues();
                GeoPos[] geoPositions = new GeoPos[selectedValues.length];
                for (int i = 0; i < selectedValues.length; i++) {
                    geoPositions[i] = (GeoPos) selectedValues[i];
                }
                listModel.removeElements(geoPositions);
            }
        });
        final JPanel buttonPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
        buttonPanel.setLayout(layout);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        return new JComponent[]{rasterScrollPane, buttonPanel};
    }

    private GeoPosDialog createGeoPosDialog() {
        final GeoPosDialog dialog = new GeoPosDialog(appContext.getApplicationWindow(), "Specify geo position",
                                                     ModalDialog.ID_OK_CANCEL, null);
        TableLayout layout = new TableLayout(2);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(0.0);
        layout.setTableWeightY(0.0);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        JPanel dialogPanel = new JPanel(layout);

        dialogPanel.add(new JLabel("Latitude"));
        final JTextField latField = new JTextField("00.0000");
        latField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    dialog.setLat(Float.parseFloat(latField.getText()));
                } catch (NumberFormatException nfe) {
                    latField.setText("00.0000");
                    dialog.setLat(00.0000f);
                }
            }
        });
        dialogPanel.add(latField);

        dialogPanel.add(new JLabel("Longitude"));
        final JTextField lonField = new JTextField("00.0000");
        lonField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                try {
                    dialog.setLon(Float.parseFloat(lonField.getText()));
                } catch (NumberFormatException nfe) {
                    lonField.setText("00.0000");
                    dialog.setLon(00.0000f);
                }
            }
        });
        dialogPanel.add(lonField);

        dialog.setContent(dialogPanel);
        return dialog;
    }

    private JSpinner createWindowSizeEditor(BindingContext bindingContext) {
        final Property squareSizeProperty = bindingContext.getPropertySet().getProperty("windowSize");
        final Number defaultValue = (Number) squareSizeProperty.getDescriptor().getDefaultValue();
        final JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 1, null, 2));
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Object value = spinner.getValue();
                if (value instanceof Integer) {
                    int intValue = (Integer) value;
                    if (intValue % 2 == 0) {
                        spinner.setValue(intValue + 1);
                    }
                }
            }
        });
        bindingContext.bind("windowSize", spinner);
        return spinner;
    }

    public JPanel getPanel() {
        return panel;
    }

    private static class GenericListModel<T> extends AbstractListModel {

        private List<T> elementList;
        private Property property;

        private GenericListModel(Property property) {
            this.property = property;
            elementList = new ArrayList<T>();
        }

        @Override
        public int getSize() {
            return elementList.size();
        }

        @Override
        public Object getElementAt(int index) {
            return elementList.get(index);
        }

        void addElement(T element) throws ValidationException {
            if (!elementList.contains(element)) {
                if (elementList.add(element)) {
                    fireIntervalAdded(this, 0, getSize());
                    updateProperty();
                }
            }
        }

        void removeElements(T... elements) {
            for (T elem : elements) {
                if (elementList.remove(elem)) {
                    fireIntervalRemoved(this, 0, getSize());
                    try {
                        updateProperty();
                    } catch (ValidationException ignored) {
                    }
                }
            }
        }

        @SuppressWarnings({"unchecked"})
        private void updateProperty() throws ValidationException {
            final T[] array = (T[]) Array.newInstance(property.getType().getComponentType(), elementList.size());
            property.setValue(elementList.toArray(array));
        }


    }

    private static class GeoPosDialog extends ModalDialog {

        private float lat;
        private float lon;

        GeoPosDialog(Window parent, String title, int buttonMask, String helpID) {
            super(parent, title, buttonMask, helpID);
        }

        public float getLat() {
            return lat;
        }

        public void setLat(float lat) {
            this.lat = lat;
        }

        public float getLon() {
            return lon;
        }

        public void setLon(float lon) {
            this.lon = lon;
        }
    }

    private static class GeoPosListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            GeoPos pos = (GeoPos) value;
            label.setText(pos.getLat() + ", " + pos.getLon());
            return label;
        }

    }
}
