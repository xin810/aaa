/*
 * Copyright (c) 2016, 2017 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.aaa.cli.dmstore;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.aaa.api.ClaimCache;
import org.opendaylight.aaa.api.IIDMStore;
import org.opendaylight.aaa.api.model.User;
import org.opendaylight.aaa.api.model.Users;
import org.opendaylight.aaa.api.password.service.PasswordHashService;
import org.opendaylight.aaa.cli.utils.CliUtils;

/**
 * ChangeUserPassword change the user password.
 *
 * @author mserngawy
 */
@Service
@Command(name = "change-user-pwd", scope = "aaa", description = "Change the user password.")
public class ChangeUserPassword implements Action {

    @Reference private IIDMStore identityStore;
    @Reference private ClaimCache claimCache;

    @Option(name = "-user", aliases = {
            "--userName" }, description = "The user name", required = true, multiValued = false)
    private String userName;

    @Reference private PasswordHashService passwordService;

    @Override
    public Object execute() throws Exception {
        if (identityStore == null) {
            return "Failed to access the users data store";
        }
        final String currentPwd = CliUtils.readPassword("Enter current password:");
        final String newPwd = CliUtils.readPassword("Enter new password:");
        final Users users = identityStore.getUsers();
        for (User usr : users.getUsers()) {
            if (usr.getName().equals(userName)
                    && passwordService.passwordsMatch(currentPwd, usr.getPassword(), usr.getSalt())) {
                claimCache.clear();
                usr.setPassword(newPwd);
                identityStore.updateUser(usr);
                return userName + "'s password has been changed";
            }
        }
        return CliUtils.LOGIN_FAILED_MESS;
    }
}
