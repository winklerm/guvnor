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
package org.guvnor.asset.management.client.editors.release;

import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.core.client.GWT;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.guvnor.asset.management.client.i18n.Constants;
import org.guvnor.asset.management.model.ProjectStructureModel;
import org.guvnor.asset.management.service.AssetManagementService;
import org.guvnor.asset.management.service.ProjectStructureService;

import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.mvp.UberView;
import org.uberfire.client.workbench.events.BeforeClosePlaceEvent;
import org.uberfire.lifecycle.OnOpen;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.security.Identity;
import org.guvnor.structure.repositories.Repository;
import org.guvnor.structure.repositories.RepositoryService;

@Dependent
@WorkbenchScreen(identifier = "Release Management")
public class ReleaseConfigurationPresenter {

    private Constants constants = GWT.create(Constants.class);

    public interface ReleaseConfigurationView extends UberView<ReleaseConfigurationPresenter> {

        void displayNotification(String text);

        ListBox getChooseRepositoryBox();

        ListBox getChooseBranchBox();

        TextBox getCurrentVersionText();

        TextBox getVersionText();

    }

    @Inject
    ReleaseConfigurationView view;

    @Inject
    Identity identity;

    @Inject
    Caller<AssetManagementService> assetManagementServices;

    @Inject
    Caller<ProjectStructureService> projectStructureServices;

    @Inject
    private Event<BeforeClosePlaceEvent> closePlaceEvent;

    private PlaceRequest place;

    @Inject
    private PlaceManager placeManager;

    @Inject
    Caller<RepositoryService> repositoryServices;

    private List<Repository> repositories;

    @OnStartup
    public void onStartup(final PlaceRequest place) {
        this.place = place;
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return constants.Release_Configuration();
    }

    @WorkbenchPartView
    public UberView<ReleaseConfigurationPresenter> getView() {
        return view;
    }

    public ReleaseConfigurationPresenter() {
    }

    @PostConstruct
    public void init() {
    }

    public void releaseProject(String repository, String branch, String project,
            String userName, String password, String serverURL, Boolean deployToMaven) {
//        assetManagementServices.call(new RemoteCallback<Long>() {
//            @Override
//            public void callback(Long taskId) {
//                view.displayNotification("Building Process Started");
//            }
//        }, new ErrorCallback<Message>() {
//            @Override
//            public boolean error(Message message, Throwable throwable) {
//                ErrorPopup.showMessage("Unexpected error encountered : " + throwable.getMessage());
//                return true;
//            }
//        }).buildProject(repository, branch, project, userName, password, serverURL, deployToMaven);

    }

    public void loadRepositories() {
        repositoryServices.call(new RemoteCallback<List<Repository>>() {

            @Override
            public void callback(final List<Repository> repositoriesResults) {
                repositories = repositoriesResults;
                view.getChooseRepositoryBox().addItem(constants.Select_Repository());
                for (Repository r : repositories) {
                    view.getChooseRepositoryBox().addItem(r.getAlias(), r.getAlias());
                }

            }
        }).getRepositories();

    }

    public void loadBranches(String repository) {
        for (Repository r : repositories) {
            if ((r.getAlias()).equals(repository)) {
                view.getChooseBranchBox().addItem(constants.Select_A_Branch());
                for (String branch : r.getBranches()) {
                    view.getChooseBranchBox().addItem(branch, branch);
                }

            }
        }
    }

    public void loadRepositoryProjectStructure(String value) {
        if (!value.equals(constants.Select_Repository())) {
            for (Repository r : repositories) {
                if ((r.getAlias()).equals(value)) {
                    projectStructureServices.call(new RemoteCallback<ProjectStructureModel>() {
                        @Override
                        public void callback(ProjectStructureModel model) {
                            if (model != null && model.getPOM() != null) {
                                view.getCurrentVersionText().setText(model.getPOM().getGav().getVersion());
                                view.getVersionText().setText(model.getPOM().getGav().getVersion());
                            } else {
                                view.getCurrentVersionText().setText(constants.No_Project_Structure_Available());
                                view.getVersionText().setText("1.0.0");
                            }
                        }
                    }).load(r);
                    return;
                }
            }
        }
    }

    @OnOpen
    public void onOpen() {
        view.getChooseRepositoryBox().setFocus(true);

    }

    public void close() {
        closePlaceEvent.fire(new BeforeClosePlaceEvent(this.place));
    }
}
