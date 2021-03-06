/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.guvnor.asset.management.client.editors.project.structure;

import com.google.gwt.user.client.ui.IsWidget;
import org.guvnor.asset.management.client.editors.project.structure.widgets.ProjectModulesView;
import org.guvnor.asset.management.client.editors.project.structure.widgets.ProjectStructureDataView;
import org.guvnor.asset.management.model.ProjectStructureModel;
import org.kie.uberfire.client.common.HasBusyIndicator;

public interface ProjectStructureView
        extends HasBusyIndicator,
        IsWidget {

    interface Presenter {

    }

    void setPresenter( ProjectStructurePresenter projectStructurePresenter );

    ProjectStructureDataView getDataView();

    ProjectModulesView getModulesView();

    void setModel( ProjectStructureModel model );

    void setModulesViewVisible( boolean visible );

    void clear();

    void setReadonly( boolean readonly );

}
