/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
 *   Copyright (C) 2016 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.joshuaglenlee.ownclient.operations;

import com.joshuaglenlee.ownclient.datamodel.OCFile;
import com.joshuaglenlee.ownclient.lib.common.OwnCloudClient;
import com.joshuaglenlee.ownclient.lib.resources.files.CreateRemoteFolderOperation;
import com.joshuaglenlee.ownclient.lib.common.operations.OnRemoteOperationListener;
import com.joshuaglenlee.ownclient.lib.common.operations.RemoteOperation;
import com.joshuaglenlee.ownclient.lib.common.operations.RemoteOperationResult;
import com.joshuaglenlee.ownclient.lib.common.utils.Log_OC;
import com.joshuaglenlee.ownclient.operations.common.SyncOperation;
import com.joshuaglenlee.ownclient.utils.FileStorageUtils;

import java.io.File;


/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server.
 * Save the new folder in Database
 */
public class CreateFolderOperation extends SyncOperation {
    
    private static final String TAG = CreateFolderOperation.class.getSimpleName();
    
    protected String mRemotePath;
    protected boolean mCreateFullPath;
    
    /**
     * Constructor
     * 
     * @param createFullPath        'True' means that all the ancestor folders should be created
     *                              if don't exist yet.
     */
    public CreateFolderOperation(String remotePath, boolean createFullPath) {
        mRemotePath = remotePath;
        mCreateFullPath = createFullPath;
        
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateRemoteFolderOperation operation = new CreateRemoteFolderOperation(mRemotePath,
                mCreateFullPath);
        RemoteOperationResult result =  operation.execute(client);
        
        if (result.isSuccess()) {
            OCFile newDir = saveFolderInDB();
            String localPath = FileStorageUtils.getDefaultSavePathFor(
                getStorageManager().getAccount().name, newDir
            );
            File localFile = new File(localPath);
            boolean created = localFile.mkdirs();
            if (!created) {
                Log_OC.w(TAG, "Local folder " + localPath + " was not fully created");
            }
        } else {
            Log_OC.e(TAG, mRemotePath + "hasn't been created");
        }
        
        return result;
    }

    /**
     * Save new directory in local database
     *
     * @return      Instance of {@link OCFile} just created
     */
    private OCFile saveFolderInDB() {
        OCFile newDir = null;
        if (mCreateFullPath && getStorageManager().
                getFileByPath(FileStorageUtils.getParentPath(mRemotePath)) == null){// When parent
                                                                                    // of remote path
                                                                                    // is not created 
            String[] subFolders = mRemotePath.split("/");
            String composedRemotePath = "/";

            // Create all the ancestors
            for (String subFolder : subFolders) {
                if (!subFolder.isEmpty()) {
                    composedRemotePath = composedRemotePath + subFolder + "/";
                    mRemotePath = composedRemotePath;
                    newDir = saveFolderInDB();
                }
            }
        } else { // Create directory on DB
            newDir = new OCFile(mRemotePath);
            newDir.setMimetype("DIR");
            long parentId = getStorageManager().
                    getFileByPath(FileStorageUtils.getParentPath(mRemotePath)).getFileId();
            newDir.setParentId(parentId);
            newDir.setModificationTimestamp(System.currentTimeMillis());
            getStorageManager().saveFile(newDir);

            Log_OC.d(TAG, "Create directory " + mRemotePath + " in Database");
        }
        return newDir;
    }
}
