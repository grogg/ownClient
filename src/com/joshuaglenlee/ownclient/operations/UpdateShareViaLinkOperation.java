/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2017 ownCloud GmbH.
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
import com.joshuaglenlee.ownclient.lib.common.operations.RemoteOperation;
import com.joshuaglenlee.ownclient.lib.common.operations.RemoteOperationResult;
import com.joshuaglenlee.ownclient.lib.resources.files.FileUtils;
import com.joshuaglenlee.ownclient.lib.resources.shares.GetRemoteShareOperation;
import com.joshuaglenlee.ownclient.lib.resources.shares.OCShare;
import com.joshuaglenlee.ownclient.lib.resources.shares.ShareType;
import com.joshuaglenlee.ownclient.lib.resources.shares.UpdateRemoteShareOperation;
import com.joshuaglenlee.ownclient.operations.common.SyncOperation;


/**
 * Updates an existing public share for a given file
 */

public class UpdateShareViaLinkOperation extends SyncOperation {

    private long mShareId;
    private String mName;
    private String mPassword;
    private Boolean mPublicUpload;
    private long mExpirationDateInMillis;

    /**
     * Constructor
     *
     * @param shareId          Local id of public share to update.
     */
    public UpdateShareViaLinkOperation(long shareId) {

        mShareId = shareId;
        mName = null;
        mPassword = null;
        mExpirationDateInMillis = 0;
        mPublicUpload = null;
    }


    /**
     * Set name to update in public link.
     *
     * @param name          Name to set to the public link.
     *                      Empty string clears the current name.
     *                      Null results in no update applied to the name.
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Set password to update in public link.
     *
     * @param password      Password to set to the public link.
     *                      Empty string clears the current password.
     *                      Null results in no update applied to the password.
     */
    public void setPassword(String password) {
        mPassword = password;
    }


    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     *                                  A negative value clears the current expiration date.
     *                                  Zero value (start-of-epoch) results in no update done on
     *                                  the expiration date.
     */
    public void setExpirationDate(long expirationDateInMillis) {
        mExpirationDateInMillis = expirationDateInMillis;
    }

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload    Upload Permission to set to the public link.
     *                        Null results in no update applied to the upload permission.
     */
    public void setPublicUpload(Boolean publicUpload) {
        mPublicUpload = publicUpload;
    }


    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OCShare storedShare = getStorageManager().getShareById(mShareId);

        if (storedShare == null || !ShareType.PUBLIC_LINK.equals(storedShare.getShareType())) {
            return new RemoteOperationResult(
                RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
            );
        }

        // Update remote share with password
        UpdateRemoteShareOperation updateOp = new UpdateRemoteShareOperation(
            storedShare.getRemoteId()
        );
        updateOp.setName(mName);
        updateOp.setPassword(mPassword);
        updateOp.setExpirationDate(mExpirationDateInMillis);
        updateOp.setPublicUpload(mPublicUpload);
        RemoteOperationResult result = updateOp.execute(client);

        if (result.isSuccess()) {
            // Retrieve updated share / save directly with password? -> no; the password is not to be saved
            RemoteOperation getShareOp = new GetRemoteShareOperation(storedShare.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                OCShare remoteShare = (OCShare) result.getData().get(0);
                updateData(storedShare, remoteShare);
            }
        }

        return result;
    }

    private void updateData(OCShare oldShare, OCShare newShare) {
        // undesired magic - TODO map remote OCShare class to proper local OCShare class
        newShare.setPath(oldShare.getPath());
        if (oldShare.getPath().endsWith(FileUtils.PATH_SEPARATOR)) {
            newShare.setIsFolder(true);
        } else {
            newShare.setIsFolder(false);
        }

        // Update DB with the response
        getStorageManager().saveShare(newShare);

        // Update OCFile with data from share
        OCFile file = getStorageManager().getFileByPath(oldShare.getPath());
        if (file != null) {
            file.setSharedViaLink(true);
            getStorageManager().saveFile(file);
        }
    }

}

