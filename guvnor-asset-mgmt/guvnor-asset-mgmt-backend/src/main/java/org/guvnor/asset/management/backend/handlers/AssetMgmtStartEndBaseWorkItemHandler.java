/*
 * Copyright 2014 JBoss Inc
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

package org.guvnor.asset.management.backend.handlers;

import java.util.List;
import java.util.Map;
import javax.enterprise.inject.spi.BeanManager;

import org.guvnor.asset.management.backend.utils.CDIUtils;
import org.guvnor.asset.management.social.ProcessEndEvent;
import org.guvnor.asset.management.social.ProcessStartEvent;
import org.guvnor.structure.repositories.RepositoryInfo;
import org.guvnor.structure.repositories.RepositoryService;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AssetMgmtStartEndBaseWorkItemHandler
        implements WorkItemHandler {

    private static final Logger logger = LoggerFactory.getLogger( AssetMgmtStartWorkItemHandler.class );

    @Override
    public void executeWorkItem( WorkItem workItem, WorkItemManager manager ) {

        String _ProcessName = ( String ) workItem.getParameter( "ProcessName" );
        String _Owner = ( String ) workItem.getParameter( "Owner" );
        String user = "system";
        String repositoryURI = null;

        //ConfigureRepository variables
        String _CB_RepositoryName;
        /* don't remove.
        String _CB_SourceBranchName;
        String _CB_DevBranchName;
        String _CB_RelBranchName;
        String _CB_Version;
        */

        //PromoteAssets variables
        String _PA_GitRepositoryName;
        /* don't remove
        String _PA_SourceBranchName;
        String _PA_TargetBranchName;
        String _PA_CommitsToPromote;
        Boolean _PA_Reviewed;
        Boolean _PA_RequiresReview;
        String _PA_ListOfCommits;
        String _PA_ListOfFiles;
        Map _PA_CommitsPerFile;
        List _PA_Commits;
        */

        //BuildProcess variables
        String _BP_ProjectURI;
        /* don't remove
        String _BP_BranchName;
        String _BP_BuildOutcome;
        List _BP_Errors;
        List _BP_Warnings;
        List _BP_Infos;
        String _BP_GAV;
        String _BP_MavenDeployOutcome;
        String _BP_ExecServerURL;
        String _BP_Username;
        String _BP_Password;
        Boolean _BP_DeployToRuntime;
        Exception _BP_Exception;
        */

        //ReleaseProject variables
        String _RP_RepositoryName;
        /* don't remove
        String _RP_Version;
        Boolean _RP_ValidForRelease;
        String _RP_ProjectURI;
        String _RP_DevBranchName;
        String _RP_RelBranchName;
        String _RP_ToReleaseDevBranch;
        String _RP_ToReleaseRelBranch;
        String _RP_ToReleaseVersion;
        Exception _RP_Exception;
        */

        BeanManager beanManager = null;
        RepositoryService repositoryService = null;
        RepositoryInfo repositoryInfo = null;

        if ( isStart() ) {
            logger.debug( "Start assets management process: " + _ProcessName + "  " + new java.util.Date() );
            System.out.println( "Start assets management process: " + _ProcessName + "  " + new java.util.Date() );
        } else {
            logger.debug( "End assets management process: " + _ProcessName + "  " + new java.util.Date() );
            System.out.println( "End assets management process: " + _ProcessName + "  " + new java.util.Date() );
        }

        try {
            beanManager = CDIUtils.lookUpBeanManager( null );
            repositoryService = CDIUtils.createBean( RepositoryService.class, beanManager );

        } catch ( Exception e ) {
            logger.debug( "BeanManager lookup error.", e );
        }

        if ( beanManager != null && "ConfigureRepository".equals( _ProcessName ) ) {

            _CB_RepositoryName = ( String ) workItem.getParameter( "CB_RepositoryName" );
            repositoryURI = readRepositoryURI( repositoryService, _CB_RepositoryName );

            if ( isStart() ) {
                ProcessStartEvent event = new ProcessStartEvent( _ProcessName, _CB_RepositoryName, repositoryURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            } else {
                ProcessEndEvent event = new ProcessEndEvent( _ProcessName, _CB_RepositoryName, repositoryURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            }

        } else if ( beanManager != null && "PromoteAssets".equals( _ProcessName ) ) {

            _PA_GitRepositoryName = ( String ) workItem.getParameter( "PA_GitRepositoryName" );
            repositoryURI = readRepositoryURI( repositoryService, _PA_GitRepositoryName );

            if ( isStart() ) {
                ProcessStartEvent event = new ProcessStartEvent( _ProcessName, _PA_GitRepositoryName, repositoryURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            } else {
                ProcessEndEvent event = new ProcessEndEvent( _ProcessName, _PA_GitRepositoryName, repositoryURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            }

        } else if ( beanManager != null && "BuildProject".equals( _ProcessName ) ) {

            _BP_ProjectURI = ( String ) workItem.getParameter( "BP_ProjectURI" );

            if ( isStart() ) {
                ProcessStartEvent event = new ProcessStartEvent( _ProcessName, _BP_ProjectURI, _BP_ProjectURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            } else {
                ProcessEndEvent event = new ProcessEndEvent( _ProcessName, _BP_ProjectURI, _BP_ProjectURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            }

        } else if ( beanManager != null && "ReleaseProject".equals( _ProcessName ) ) {

            _RP_RepositoryName = ( String ) workItem.getParameter( "RP_RepositoryName" );
            repositoryURI = readRepositoryURI( repositoryService, _RP_RepositoryName );

            if ( isStart() ) {
                ProcessStartEvent event = new ProcessStartEvent( _ProcessName, _RP_RepositoryName, repositoryURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            } else {
                ProcessEndEvent event = new ProcessEndEvent( _ProcessName, _RP_RepositoryName, repositoryURI, user, System.currentTimeMillis() );
                beanManager.fireEvent( event );
            }

        }

        if ( manager != null ) {
            manager.completeWorkItem( workItem.getId(), null );
        }
    }

    String readRepositoryURI(RepositoryService repositoryService, String alias) {
        String uri = null;
        RepositoryInfo repositoryInfo = alias != null ? repositoryService.getRepositoryInfo( alias ) : null;
        if ( repositoryInfo != null && repositoryInfo.getRoot() != null ) {
            uri = repositoryInfo.getRoot().toURI();
        }
        return uri;
    }

    @Override public void abortWorkItem( WorkItem workItem, WorkItemManager manager ) {
        //do nothing
    }

    protected abstract boolean isStart();
}
