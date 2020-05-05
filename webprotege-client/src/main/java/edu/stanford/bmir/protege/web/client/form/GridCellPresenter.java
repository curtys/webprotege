package edu.stanford.bmir.protege.web.client.form;

import com.google.common.collect.ImmutableList;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import edu.stanford.bmir.protege.web.client.library.dlg.HasRequestFocus;
import edu.stanford.bmir.protege.web.shared.form.FormRegionPageChangedHandler;
import edu.stanford.bmir.protege.web.shared.form.FormRegionPageRequest;
import edu.stanford.bmir.protege.web.shared.form.HasFormRegionPagedChangedHandler;
import edu.stanford.bmir.protege.web.shared.form.data.GridCellData;
import edu.stanford.bmir.protege.web.shared.form.field.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.shared.form.field.Optionality.REQUIRED;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-11-25
 */
public class GridCellPresenter implements HasRequestFocus, HasFormRegionPagedChangedHandler, HasGridColumnVisibilityManager {

    @Nonnull
    private final GridCellView view;

    @Nonnull
    private Optional<GridColumnDescriptor> descriptor = Optional.empty();

    @Nonnull
    private final FormFieldControlStackFactory formFieldControlStackFactory;

    /**
     * Multiple values are allowed per cell, so we use a control stack
     */
    private FormControlStack controlStack;

    private boolean enabled = true;

    private Optional<GridColumnVisibilityManager> columnVisibilityManager = Optional.empty();

    @Inject
    public GridCellPresenter(@Nonnull GridCellView view,
                             @Nonnull FormFieldControlStackFactory formFieldControlStackFactory) {
        this.view = checkNotNull(view);
        this.formFieldControlStackFactory = checkNotNull(formFieldControlStackFactory);
    }

    public void clear() {
        controlStack.clearValue();
        updateValueRequired();
    }

    @Nonnull
    public ImmutableList<FormRegionPageRequest> getPageRequest() {
        return ImmutableList.of();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        controlStack.setEnabled(enabled);
    }

    @Override
    public void setFormRegionPageChangedHandler(@Nonnull FormRegionPageChangedHandler handler) {
        controlStack.setFormRegionPageChangedHandler(handler);
    }

    public Optional<GridColumnId> getId() {
        return descriptor.map(GridColumnDescriptor::getId);
    }

    public void requestFocus() {
        view.requestFocus();
    }

    public void setDescriptor(GridColumnDescriptor column) {
        FormControlDescriptor formControlDescriptor = column.getFormControlDescriptor();
        controlStack = formFieldControlStackFactory.create(formControlDescriptor,
                                                           column.getRepeatability(),
                                                           FormRegionPosition.NESTED);
        controlStack.setEnabled(enabled);
        view.getEditorContainer().setWidget(controlStack);
        this.descriptor = Optional.of(column);
    }

    public void start(AcceptsOneWidget cellContainer) {
        cellContainer.setWidget(view);
    }

    public void setValue(GridCellData data) {
        controlStack.clearValue();
        controlStack.setValue(data.getValues());
        controlStack.setEnabled(enabled);
        updateValueRequired();
        updateColumnVisibilityManagerInChildControls();
    }

    public GridCellData getValue() {
        if(!descriptor.isPresent()) {
            return GridCellData.get(GridColumnId.get("null"), ImmutableList.of());
        }
        GridColumnDescriptor columnDescriptor = descriptor.get();
        return controlStack.getValue().map(v -> GridCellData.get(columnDescriptor.getId(),
                                                                 ImmutableList.copyOf(v)))
                           .orElse(GridCellData.get(columnDescriptor.getId(), ImmutableList.of()));
    }

    public boolean isPresent() {
        return getId().isPresent();
    }

    private void updateValueRequired() {
        descriptor.ifPresent(descriptor -> {
            if (descriptor.getOptionality() == REQUIRED) {
                boolean requiredValueNotPresent = !controlStack.getValue().isPresent();
                view.setRequiredValueNotPresentVisible(requiredValueNotPresent);
            }
            else {
                view.setRequiredValueNotPresentVisible(false);
            }
        });
    }

    public void updateColumnVisibility() {
        controlStack.forEachFormControl(formControl -> {
            if(formControl instanceof GridControl) {
                ((GridControl) formControl).updateColumnVisibility();
            }
        });
    }

    @Override
    public void setColumnVisibilityManager(@Nonnull GridColumnVisibilityManager columnVisibilityManager) {
        this.columnVisibilityManager = Optional.of(checkNotNull(columnVisibilityManager));
        updateColumnVisibilityManagerInChildControls();
    }

    private void updateColumnVisibilityManagerInChildControls() {
        columnVisibilityManager.ifPresent(visibilityManager -> {
            controlStack.forEachFormControl(formControl -> {
                if(formControl instanceof GridControl) {
                    ((GridControl) formControl).setColumnVisibilityManager(visibilityManager);
                }
            });
        });
    }

}