package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.util.List;
import java.util.function.DoubleFunction;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public abstract class AbstractHeatmapWidget extends WidgetDelegate
{
    private Composite table;
    private Label title;
    private DashboardResources resources;

    public AbstractHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new ColorSchemaConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        this.resources = resources;

        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        table = new Composite(container, SWT.NONE);
        // 13 columns, one for the legend and 12 for the months
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        table.setBackground(container.getBackground());

        fillTable(table, resources);

        return container;
    }

    protected abstract HeatmapModel build();

    private void fillTable(Composite table, DashboardResources resources)
    {
        HeatmapModel model = build();

        GridLayoutFactory.fillDefaults().numColumns(model.getHeaderSize() + 1).equalWidth(true).spacing(1, 1)
                        .applyTo(table);

        addHeaderRow(table, model);

        DoubleFunction<Color> coloring = get(ColorSchemaConfig.class).getValue()
                        .buildColorFunction(resources.getResourceManager());

        GridDataFactory gridData = GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL);

        model.getRows().forEach(row -> {
            Cell cell = new Cell(table, new CellDataProvider(row.getLabel()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cell);

            row.getData().forEach(data -> {
                Cell dataCell = data == null ? new Cell(table, new CellDataProvider("")) //$NON-NLS-1$
                                : new Cell(table, new CellDataProvider(coloring.apply(data), resources.getSmallFont(),
                                                Values.PercentShort.format(data)));
                gridData.applyTo(dataCell);

                if (model.getCellToolTip() != null)
                    InfoToolTip.attach(dataCell, model.getCellToolTip());
            });
        });

        table.layout(true);
    }

    private void addHeaderRow(Composite table, HeatmapModel model)
    {
        // Top Left is empty
        new Cell(table, new CellDataProvider("")); //$NON-NLS-1$

        GridDataFactory gridData = GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL);
        model.getHeader().forEach(label -> {
            Cell cell = new Cell(table, new CellDataProvider(label));
            gridData.applyTo(cell);
            InfoToolTip.attach(cell, label);
        });
    }

    @Override
    public void update()
    {
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$

        for (Control child : table.getChildren())
            child.dispose();

        fillTable(table, resources);

        table.getParent().layout(true);
        table.getParent().getParent().layout(true);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    protected Double geometricMean(List<Double> values)
    {
        if (values.isEmpty())
            return null;

        if (values.size() == 1)
            return values.get(0);

        double sum = 1;
        for (Double v : values)
            sum *= (1 + v);

        return Math.pow(sum, 1 / (double) values.size()) - 1;
    }
}
