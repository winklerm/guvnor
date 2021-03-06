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

import java.util.List;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import org.guvnor.asset.management.client.editors.project.structure.widgets.ProjectModuleRow;
import org.guvnor.asset.management.client.editors.project.structure.widgets.ProjectModulesView;
import org.guvnor.asset.management.client.editors.project.structure.widgets.ProjectStructureDataView;
import org.guvnor.asset.management.client.i18n.Constants;
import org.guvnor.asset.management.model.ProjectStructureModel;
import org.guvnor.asset.management.service.ProjectStructureService;
import org.guvnor.common.services.project.builder.model.BuildResults;
import org.guvnor.common.services.project.builder.service.BuildService;
import org.guvnor.common.services.project.context.ProjectContext;
import org.guvnor.common.services.project.context.ProjectContextChangeEvent;
import org.guvnor.common.services.project.model.GAV;
import org.guvnor.common.services.project.model.POM;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.common.services.project.model.ProjectWizard;
import org.guvnor.common.services.project.service.POMService;
import org.guvnor.structure.repositories.Repository;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.container.IOC;
import org.kie.uberfire.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.kie.uberfire.client.common.popups.YesNoCancelPopup;
import org.kie.uberfire.client.resources.i18n.CommonConstants;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.annotations.WorkbenchPartTitle;
import org.uberfire.client.annotations.WorkbenchPartView;
import org.uberfire.client.annotations.WorkbenchScreen;
import org.uberfire.client.callbacks.Callback;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.workbench.events.BeforeClosePlaceEvent;
import org.uberfire.client.workbench.events.ChangeTitleWidgetEvent;
import org.uberfire.lifecycle.OnClose;
import org.uberfire.lifecycle.OnFocus;
import org.uberfire.lifecycle.OnStartup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.ParameterizedCommand;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;

import static org.kie.uberfire.client.common.ConcurrentChangePopup.*;

@WorkbenchScreen( identifier = "projectStructureScreen" )
public class ProjectStructurePresenter
        implements ProjectStructureView.Presenter,
        ProjectStructureDataView.Presenter,
        ProjectModulesView.Presenter {

    private ProjectStructureView view;

    @Inject
    private Caller<ProjectStructureService> projectStructureService;

    @Inject
    private Caller<POMService> pomService;

    @Inject
    private Caller<BuildService> buildServiceCaller;

    @Inject
    private Event<BuildResults> buildResultsEvent;

    @Inject
    private Event<NotificationEvent> notificationEvent;

    @Inject
    private Event<ChangeTitleWidgetEvent> changeTitleWidgetEvent;

    @Inject
    private Event<BeforeClosePlaceEvent> beforeCloseEvent;

    @Inject
    private Event<ProjectContextChangeEvent> contextChangeEvent;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private ProjectContext workbenchContext;

    private ProjectStructureModel model;

    private Project project;

    private Project lastAddedModule;

    private Project lastDeletedModule;

    private Repository repository;

    private String branch;

    private ObservablePath pathToProjectStructure;

    private PlaceRequest placeRequest;

    private ObservablePath.OnConcurrentUpdateEvent concurrentUpdateSessionInfo = null;

    private ListDataProvider<ProjectModuleRow> dataProvider = new ListDataProvider<ProjectModuleRow>();

    @Inject
    private ProjectWizard wizzard;

    @Inject
    public ProjectStructurePresenter( ProjectStructureView view ) {
        this.view = view;
        view.setPresenter( this );
        view.getDataView().setPresenter( this );
        view.getModulesView().setPresenter( this );
    }

    @OnStartup
    public void onStartup( final PlaceRequest placeRequest ) {
        this.placeRequest = placeRequest;
        processContextChange( workbenchContext.getActiveRepository(), workbenchContext.getActiveProject() );
    }

    @WorkbenchPartTitle
    public String getTitle() {
        return Constants.INSTANCE.ProjectStructure();
    }

    @WorkbenchPartView
    public IsWidget asWidget() {
        return view.asWidget();
    }

    @OnClose
    public void onClose() {
        concurrentUpdateSessionInfo = null;
        if ( pathToProjectStructure != null ) {
            pathToProjectStructure.dispose();
        }
    }

    @OnFocus
    public void onFocus() {
        //workaround.
        dataProvider.flush();
        dataProvider.refresh();
    }

    private void onContextChange( @Observes final ProjectContextChangeEvent event ) {
        processContextChange( event.getRepository(), event.getProject() );
    }

    private void processContextChange( final Repository repository, final Project project ) {
        boolean repoOrBranchChanged = false;

        if ( repository == null ) {
            clearView();
            view.setReadonly( true );
            view.setModulesViewVisible( false );
            enableActions( false );
        } else if ( ( repoOrBranchChanged = repositoryOrBranchChanged( repository ) ) || (project != null && !project.equals( this.project ) ) ) {

            this.repository = repository;
            this.branch = repository != null ? repository.getCurrentBranch() : null;
            this.project = project;

            view.setReadonly( false );
            if ( repoOrBranchChanged || (lastAddedModule == null || !lastAddedModule.equals( project )) && lastDeletedModule == null) {
                init();
            }
            lastAddedModule = null;
            lastDeletedModule = null;
        }
    }

    private void init() {

        view.showBusyIndicator( Constants.INSTANCE.Loading() );
        clearView();
        projectStructureService.call( getLoadModelSuccessCallback(),
                new HasBusyIndicatorDefaultErrorCallback( view ) ).load( repository );

    }

    private RemoteCallback<ProjectStructureModel> getLoadModelSuccessCallback() {

        return new RemoteCallback<ProjectStructureModel>() {
            @Override
            public void callback( final ProjectStructureModel model ) {

                view.hideBusyIndicator();

                ProjectStructurePresenter.this.model = model;
                dataProvider.getList().clear();
                if ( pathToProjectStructure != null ) {
                    pathToProjectStructure.dispose();
                }
                concurrentUpdateSessionInfo = null;

                if ( model == null ) {

                    ProjectStructurePresenter.this.model = new ProjectStructureModel();
                    view.getDataView().setMode( ProjectStructureDataView.ViewMode.CREATE_STRUCTURE );
                    view.getModulesView().setMode( ProjectModulesView.ViewMode.PROJECTS_VIEW );
                    view.setModel( ProjectStructurePresenter.this.model );
                    view.setModulesViewVisible( false );
                    pathToProjectStructure = null;

                } else if ( model.isMultiModule() ) {

                    view.getDataView().setMode( ProjectStructureDataView.ViewMode.EDIT_MULTI_MODULE_PROJECT );
                    view.getModulesView().setMode( ProjectModulesView.ViewMode.PROJECTS_VIEW );
                    view.setModel( model );
                    view.setModulesViewVisible( true );

                    pathToProjectStructure = IOC.getBeanManager().lookupBean( ObservablePath.class ).getInstance().wrap( model.getPathToPOM() );

                    updateModulesList( model.getModules() );

                } else if ( model.isSingleProject() ) {

                    view.getDataView().setMode( ProjectStructureDataView.ViewMode.EDIT_SINGLE_MODULE_PROJECT );
                    view.getModulesView().setMode( ProjectModulesView.ViewMode.PROJECTS_VIEW );
                    view.setModel( model );
                    view.setModulesViewVisible( false );

                    pathToProjectStructure = IOC.getBeanManager().lookupBean( ObservablePath.class ).getInstance().wrap( model.getOrphanProjects().get( 0 ).getPomXMLPath() );

                } else {

                    view.getDataView().setMode( ProjectStructureDataView.ViewMode.EDIT_UNMANAGED_REPOSITORY );
                    view.getModulesView().setMode( ProjectModulesView.ViewMode.PROJECTS_VIEW );
                    view.setModulesViewVisible( true );

                    view.setModel( model );
                    updateProjectsList( model.getOrphanProjects() );
                }

                addStructureChangeListeners();
                updateEditorTitle();
            }
        };
    }

    private void reload() {
        concurrentUpdateSessionInfo = null;
        init();
    }

    private void initProjectStructure() {
        //TODO add parameters validation

        if ( view.getDataView().isMultiModule() ) {
            view.showBusyIndicator( Constants.INSTANCE.CreatingProjectStructure() );
            projectStructureService.call( new RemoteCallback<Path>() {

                @Override
                public void callback( Path response ) {
                    view.hideBusyIndicator();
                    init();
                }

            } ).initProjectStructure( new GAV( view.getDataView().getGroupId(),
                    view.getDataView().getArtifactId(),
                    view.getDataView().getVersionId() ),
                    this.repository );
        } else {
            wizzard.setContent( null, null, null);
            wizzard.start( new Callback<Project>() {
                @Override public void callback( Project result ) {
                    lastAddedModule = result;
                    if ( result != null ) {
                        init();
                    }
                }
            }, false);
        }
    }

    private void updateEditorTitle() {

        if ( repository == null ) {

            changeTitleWidgetEvent.fire( new ChangeTitleWidgetEvent( placeRequest,
                    Constants.INSTANCE.RepositoryNotSelected() ) );

        } else if ( model == null ) {

            changeTitleWidgetEvent.fire( new ChangeTitleWidgetEvent(
                    placeRequest,
                    Constants.INSTANCE.ProjectStructureWithName( getRepositoryLabel( repository ) ) ) );

        } else if ( model.isMultiModule() ) {

            changeTitleWidgetEvent.fire( new ChangeTitleWidgetEvent(
                    placeRequest,
                    Constants.INSTANCE.ProjectStructureWithName( getRepositoryLabel( repository ) + "- > " +
                            model.getPOM().getGav().getArtifactId() + ":"
                            + model.getPOM().getGav().getGroupId() + ":"
                            + model.getPOM().getGav().getVersion() ) ) );

        } else if ( model.isSingleProject() ) {
            changeTitleWidgetEvent.fire( new ChangeTitleWidgetEvent(
                    placeRequest,
                    Constants.INSTANCE.ProjectStructureWithName( getRepositoryLabel( repository ) + "- > " + model.getOrphanProjects().get( 0 ).getProjectName() ) ) );

        } else {
            changeTitleWidgetEvent.fire( new ChangeTitleWidgetEvent(
                    placeRequest,
                    Constants.INSTANCE.UnmanagedRepository( getRepositoryLabel( repository ) ) ) );
        }
    }

    private String getRepositoryLabel(Repository repository) {
        return repository != null ? ( repository.getAlias() + " (" + repository.getCurrentBranch() + ") " ) : "";
    }

    private void addStructureChangeListeners() {

        if ( pathToProjectStructure != null ) {

            pathToProjectStructure.onConcurrentUpdate( new ParameterizedCommand<ObservablePath.OnConcurrentUpdateEvent>() {
                @Override
                public void execute( final ObservablePath.OnConcurrentUpdateEvent eventInfo ) {
                    concurrentUpdateSessionInfo = eventInfo;
                }
            } );

            pathToProjectStructure.onConcurrentRename( new ParameterizedCommand<ObservablePath.OnConcurrentRenameEvent>() {
                @Override
                public void execute( final ObservablePath.OnConcurrentRenameEvent info ) {
                    newConcurrentRename( info.getSource(),
                            info.getTarget(),
                            info.getIdentity(),
                            new Command() {
                                @Override
                                public void execute() {
                                    enableActions( false );
                                }
                            },
                            new Command() {
                                @Override
                                public void execute() {
                                    reload();
                                }
                            }
                    ).show();
                }
            } );

            pathToProjectStructure.onConcurrentDelete( new ParameterizedCommand<ObservablePath.OnConcurrentDelete>() {
                @Override
                public void execute( final ObservablePath.OnConcurrentDelete info ) {
                    newConcurrentDelete( info.getPath(),
                            info.getIdentity(),
                            new Command() {
                                @Override
                                public void execute() {
                                    enableActions( false );
                                }
                            },
                            new Command() {
                                @Override
                                public void execute() {
                                    placeManager.closePlace( "projectStructureScreen" );
                                }
                            }
                    ).show();
                }
            } );
        }
    }

    private void updateModulesList( List<String> modules ) {
        if ( modules != null ) {
            for ( String module : model.getModules() ) {
                dataProvider.getList().add( new ProjectModuleRow( module ) );
            }
        }
    }

    private void updateProjectsList( List<Project> projects ) {
        if ( projects != null ) {
            for ( Project project : projects ) {
                dataProvider.getList().add( new ProjectModuleRow( project.getProjectName() ) );
            }
        }
    }

    private void enableActions( boolean value ) {
        view.getDataView().enableActions( value );
        view.getModulesView().enableActions( value );
    }

    private void clearView() {
        view.getDataView().clear();
        dataProvider.getList().clear();
        enableActions( true );
    }

    /**
     * *** Presenter interfaces *******
     */

    @Override
    public void onAddModule() {
        if ( model.isSingleProject() || model.isMultiModule() ) {
            wizzard.setContent( null,
                    view.getDataView().getGroupId(),
                    view.getDataView().getVersionId() );
        } else if ( model.isOrphanProjects() ) {
            wizzard.setContent( null,
                    null,
                    null );

        }

        wizzard.start( getModuleAddedSuccessCallback(), false );
    }

    private Callback<Project> getModuleAddedSuccessCallback() {
        //optimization to avoid reloading the complete model when a module is added.
        return new Callback<Project>() {
            @Override
            public void callback( final Project _project ) {
                lastAddedModule = _project;
                if ( _project != null ) {
                    //A new module was added.
                    if ( model.isMultiModule() ) {
                        view.showBusyIndicator( Constants.INSTANCE.Loading() );
                        projectStructureService.call( new RemoteCallback<ProjectStructureModel>() {
                            @Override
                            public void callback( ProjectStructureModel _model ) {
                                view.hideBusyIndicator();
                                if ( _model != null ) {
                                    model.setPOM( _model.getPOM() );
                                    model.setPOMMetaData( _model.getPOMMetaData() );
                                    model.setModules( _model.getModules() );
                                    model.getModulesProject().put( _project.getProjectName(), _project );
                                    addToModulesList( _project );
                                }
                            }
                        }, new HasBusyIndicatorDefaultErrorCallback( view ) ).load( repository, false );

                    } else if ( model.isOrphanProjects() ) {
                        view.showBusyIndicator( Constants.INSTANCE.Loading() );
                        pomService.call( new RemoteCallback<POM>() {
                            @Override
                            public void callback( POM _pom ) {
                                view.hideBusyIndicator();
                                model.getOrphanProjects().add( _project );
                                model.getOrphanProjectsPOM().put( _project.getSignatureId(), _pom );
                                addToModulesList( _project );
                            }
                        }, new HasBusyIndicatorDefaultErrorCallback( view ) ).load( _project.getPomXMLPath() );
                    } else {
                        init();
                    }
                }
            }
        };
    }

    @Override
    public void addDataDisplay( HasData<ProjectModuleRow> display ) {
        dataProvider.addDataDisplay( display );
    }

    @Override
    public void onDeleteModule( ProjectModuleRow moduleRow ) {

        final Project project = getSelectedModule( moduleRow.getName() );
        String message = null;

        if ( project != null ) {

            if ( model.isSingleProject() || model.isMultiModule() ) {
                message = Constants.INSTANCE.ConfirmModuleDeletion( moduleRow.getName() );
            } else if ( model.isOrphanProjects() ) {
                message = Constants.INSTANCE.ConfirmProjectDeletion( moduleRow.getName() );
            }

            YesNoCancelPopup yesNoCancelPopup = YesNoCancelPopup.newYesNoCancelPopup( CommonConstants.INSTANCE.Information(),
                    message,
                    new Command() {
                        @Override
                        public void execute() {
                            deleteSelectedModule( project );
                        }
                    },
                    CommonConstants.INSTANCE.YES(),
                    ButtonType.DANGER,
                    IconType.MINUS_SIGN,
                    new Command() {
                        @Override public void execute() {
                            //do nothing
                        }
                    },
                    null,
                    ButtonType.DEFAULT,
                    null,
                    new Command() {
                        @Override public void execute() {
                            //do nothing.
                        }
                    },
                    null,
                    ButtonType.DEFAULT,
                    null
            );

            yesNoCancelPopup.setCloseVisible( false );
            yesNoCancelPopup.show();
        }
    }

    private void deleteSelectedModule( final Project project ) {

        view.showBusyIndicator( Constants.INSTANCE.Deleting() );
        lastDeletedModule = project;
        projectStructureService.call( getModuleDeletedSuccessCallback( project )
                , new HasBusyIndicatorDefaultErrorCallback( view ) ).delete( project.getPomXMLPath(), "Module removed" );
    }

    private RemoteCallback<Void> getModuleDeletedSuccessCallback( final Project _project ) {
        //optimization to avoid reloading the complete model when a module is added.
        return new RemoteCallback<Void>() {
            @Override
            public void callback( Void response ) {
                if ( _project != null ) {
                    //A project was deleted
                    if ( model.isMultiModule() ) {
                        view.showBusyIndicator( Constants.INSTANCE.Loading() );
                        projectStructureService.call( new RemoteCallback<ProjectStructureModel>() {
                            @Override
                            public void callback( ProjectStructureModel _model ) {
                                view.hideBusyIndicator();
                                if ( _model != null ) {
                                    model.setPOM( _model.getPOM() );
                                    model.setPOMMetaData( _model.getPOMMetaData() );
                                    model.setModules( _model.getModules() );
                                    model.getModulesProject().remove( _project.getProjectName() );
                                    removeFromModulesList( _project.getProjectName() );
                                }
                            }
                        }, new HasBusyIndicatorDefaultErrorCallback( view ) ).load( repository, false );

                    } else if ( model.isOrphanProjects() ) {
                        view.showBusyIndicator( Constants.INSTANCE.Loading() );
                        pomService.call( new RemoteCallback<POM>() {
                            @Override
                            public void callback( POM _pom ) {
                                view.hideBusyIndicator();
                                model.getOrphanProjects().remove( _project );
                                model.getOrphanProjectsPOM().remove( _project.getSignatureId() );
                                removeFromModulesList( _project.getProjectName() );
                            }
                        }, new HasBusyIndicatorDefaultErrorCallback( view ) ).load( _project.getPomXMLPath() );
                    } else {
                        init();
                    }
                }
            }
        };
    }

    @Override
    public void onEditModule( ProjectModuleRow moduleRow ) {

        Project project = getSelectedModule( moduleRow.getName() );
        if ( project != null ) {
            //TODO check if there's a better implementation for this projectScreen opening.
            contextChangeEvent.fire( new ProjectContextChangeEvent( workbenchContext.getActiveOrganizationalUnit(), repository, project ) );
            placeManager.goTo( "projectScreen" );
        }
    }

    @Override
    public void onArtifactIdChange( String artifactId ) {
        //Window.alert( "onArtifactIdChange: " + artifactId );
    }

    @Override
    public void onGroupIdChange( String groupId ) {
        //Window.alert( "onGroupIdChange: " + groupId );
    }

    @Override
    public void onVersionChange( String version ) {
        //Window.alert( "onVersionChange: " + version );
    }

    @Override
    public void onProjectModeChange( boolean isSingle ) {

    }

    @Override
    public void onInitProjectStructure() {
        initProjectStructure();
    }

    @Override
    public void onSaveProjectStructure() {

        if ( model.getPOM() != null ) {

            YesNoCancelPopup yesNoCancelPopup = YesNoCancelPopup.newYesNoCancelPopup( CommonConstants.INSTANCE.Information(),
                    Constants.INSTANCE.ConfirmSaveProjectStructure(),
                    new Command() {
                        @Override
                        public void execute() {
                            saveProjectStructure();
                        }
                    },
                    CommonConstants.INSTANCE.YES(),
                    ButtonType.PRIMARY,
                    IconType.SAVE,
                    new Command() {
                        @Override public void execute() {
                            //do nothing
                        }
                    },
                    null,
                    ButtonType.DEFAULT,
                    null,
                    new Command() {
                        @Override public void execute() {
                            //do nothing.
                        }
                    },
                    null,
                    ButtonType.DEFAULT,
                    null
            );

            yesNoCancelPopup.setCloseVisible( false );
            yesNoCancelPopup.show();
        }
    }

    private void saveProjectStructure() {

        if ( model.getPOM() != null ) {

            model.getPOM().getGav().setGroupId( view.getDataView().getGroupId() );
            model.getPOM().getGav().setArtifactId( view.getDataView().getArtifactId() );
            model.getPOM().getGav().setVersion( view.getDataView().getVersionId() );

            view.showBusyIndicator( Constants.INSTANCE.Saving() );
            projectStructureService.call( new RemoteCallback<Void>() {
                @Override
                public void callback( Void response ) {
                    view.hideBusyIndicator();
                    init();
                }
            } ).save( model.getPathToPOM(), model, "" );
        }
    }

    @Override
    public void onConvertToMultiModule() {

        YesNoCancelPopup yesNoCancelPopup = YesNoCancelPopup.newYesNoCancelPopup( CommonConstants.INSTANCE.Information(),
                Constants.INSTANCE.ConfirmConvertToMultiModuleStructure(),
                new Command() {
                    @Override
                    public void execute() {
                        convertToMultiModule();
                    }
                },
                CommonConstants.INSTANCE.YES(),
                ButtonType.PRIMARY,
                IconType.SAVE,
                new Command() {
                    @Override public void execute() {
                        //do nothing
                    }
                },
                null,
                ButtonType.DEFAULT,
                null,
                new Command() {
                    @Override public void execute() {
                        //do nothing.
                    }
                },
                null,
                ButtonType.DEFAULT,
                null
        );

        yesNoCancelPopup.setCloseVisible( false );
        yesNoCancelPopup.show();

    }

    private void convertToMultiModule() {
        Project project = model.getOrphanProjects().get( 0 );
        POM pom = model.getOrphanProjectsPOM().get( project.getSignatureId() );
        GAV gav = new GAV( view.getDataView().getGroupId(), view.getDataView().getArtifactId(), view.getDataView().getVersionId() );

        view.showBusyIndicator( Constants.INSTANCE.ConvertingToMultiModuleProject() );
        projectStructureService.call( new RemoteCallback<Path>() {
            @Override
            public void callback( Path response ) {
                view.hideBusyIndicator();
                init();
            }
        } ).convertToMultiProjectStructure( model.getOrphanProjects(), gav, repository, true, null );
    }

    @Override
    public void onOpenSingleProject() {
        if ( model != null && model.isSingleProject() ) {
            placeManager.goTo( "projectScreen" );
        }
    }

    private Project getSelectedModule( String name ) {
        Project project = null;
        if ( model != null && name != null ) {
            if ( model.isSingleProject() || model.isMultiModule() ) {
                project = model.getModulesProject() != null ? model.getModulesProject().get( name ) : null;
            } else if ( model.isOrphanProjects() ) {
                for ( Project _project : model.getOrphanProjects() ) {
                    if ( name.equals( _project.getProjectName() ) ) {
                        project = _project;
                        break;
                    }
                }
            }
        }
        return project;
    }

    private void removeFromModulesList( String module ) {
        if ( module != null ) {
            int index = -1;
            for ( ProjectModuleRow row : dataProvider.getList() ) {
                index++;
                if ( module.equals( row.getName() ) ) {
                    break;
                }
            }
            if ( index >= 0 && ( index == 0 || index < dataProvider.getList().size() ) ) {
                dataProvider.getList().remove( index );
            }
        }
    }

    private void addToModulesList( final Project project ) {
        dataProvider.getList().add( new ProjectModuleRow( project.getProjectName() ) );
    }

    private boolean repositoryOrBranchChanged( Repository selectedRepository ) {
        return selectedRepository != null &&
                ( !selectedRepository.equals( this.repository ) ||
                  !selectedRepository.getCurrentBranch().equals( this.branch )
                );
    }
}