#!/usr/bin/env bash

# find group
function group_find(){

    out="$(ipa group-show $GROUPNAME)"

    [[ "$?" != "0" ]] && echo "Group:$GROUPNAME does not exist" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Group:$GROUPNAME exists"
    return 0

}

# find user
function user_find(){

    out="$(ipa user-show $USERNAME)"

    [[ "$?" != "0" ]] && echo "User:$USERNAME does not exist" && return 1

    [[ "$DEBUG" == "true" ]] && echo "USER:$USERNAME exists"

    return 0

}



# Add User
function add_user() {
    out="$(echo $PASSWORD | kinit $ADMINUSER )"
    [[ "$?" != "0" ]] && echo "Error while kinit, invalid user or password : $ADMINUSER" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Admin User=$ADMINUSER";
    [[ "$DEBUG" == "true" ]] && echo "Username to be added =[$USERNAME, $FNAME $LNAME]";


    user_find
    r=$?
    #echo "return code:$r"
    [[ "$r" == "0" ]] && echo "User $USERNAME already exist" && return 0;

    out="$(ipa user-add $USERNAME --first=$FNAME --last=$LNAME --cn='$FNAME $LNAME' )"
    r=$?

    [[ "$r" != "0" ]] && echo "Error while Adding User $USERNAME" && return 1;

    echo "User Added:" $out
    return 0

}


# del user
function del_user(){
    out="$(echo $PASSWORD | kinit $ADMINUSER )"

    [[ "$?" != "0" ]] && echo "Error while kinit, invalid user or password : $ADMINUSER" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Admin User=$ADMINUSER";
    [[ "$DEBUG" == "true" ]] && echo "Username to be added =[$USERNAME, $FNAME $LNAME]";

    user_find
    r=$?
    #echo "return code:$r"
    [[ "$r" != "0" ]] && echo "User $USERNAME not found, doing nothing" && return 0;


    out="$(ipa user-del $USERNAME)"
    r=$?
    [[ "$r" != "0" ]] && echo "Error while deleting User $USERNAME" && return 1;

    echo "User removed:" $out
    return 0
}

# change user password
function user_change_password(){
    out="$(echo $PASSWORD | kinit $ADMINUSER )"

    [[ "$?" != "0" ]] && echo "Error while kinit, invalid user or password : $ADMINUSER" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Admin User=$ADMINUSER";
    [[ "$DEBUG" == "true" ]] && echo "Password to be changed for [$USERNAME]";

    user_find
    r=$?
    #echo "return code:$r"
    [[ "$r" != "0" ]]  && echo "User $USERNAME not found, doing nothing" && return 1;


    [[ "$DEBUG" == "true" ]] && echo "[$USERPASSWORD]"

    out=$(printf "$USERPASSWORD\n$USERPASSWORD" | ipa passwd $USERNAME)
    r=$?
    [[ "$r" != "0" ]] && echo "Error while Changing Password of User $USERNAME" && return 1;

    echo "User's Password updated:" $out
    return 0

}

# group add
function group_add(){
    out="$(echo $PASSWORD | kinit $ADMINUSER )"

    [[ "$?" != "0" ]] && echo "Error while kinit, invalid user or password : $ADMINUSER" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Admin User=$ADMINUSER";
    [[ "$DEBUG" == "true" ]] && echo "Group to be added =[$GROUPNAME]";


    # check if the group exists or not.
    group_find
    r=$?
    [[ "$r" == "0" ]] && echo "Group $GROUPNAME already exists, doing nothing" && return 0;

    echo"adding grp"
    out="$(ipa group-add $GROUPNAME)"
    r=$?
    [[ "$r" != "0" ]] && echo "Error while adding group $GROUPNAME" && return 1;

    echo "Group Added:" $out
    return 0

}

# group del
function group_del(){
    out="$(echo $PASSWORD | kinit $ADMINUSER )"

    [[ "$?" != "0" ]] && echo "Error while kinit, invalid user or password : $ADMINUSER" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Admin User=$ADMINUSER";
    [[ "$DEBUG" == "true" ]] && echo "Group to be added =[$GROUPNAME]";


    # check if the group exists or not.
    group_find
    r=$?
    [[ "$r" != "0" ]] && echo "Group $GROUPNAME not found, doing nothing" && return 0;

    echo "removing grp"
    err="$(ipa group-del $GROUPNAME 2>&1)"
    r=$?
    [[ "$r" != "0" ]] && echo "Error while deleting group $GROUPNAME $err" && return 1;

    echo "Group deleted:" $out
    return 0

}


# user group assign
function user_add_group(){
    out="$(echo $PASSWORD | kinit $ADMINUSER )"

    [[ "$?" != "0" ]] && echo "Error while kinit, invalid user or password : $ADMINUSER" && return 1;

    [[ "$DEBUG" == "true" ]] && echo "Admin User=$ADMINUSER";
    [[ "$DEBUG" == "true" ]] && echo "Username to be added =[$USERNAME, $FNAME $LNAME]";

    user_find
    r=$?
    [[ "$r" != "0" ]] && echo "User $USERNAME not found" && return 1;


    # check if the group exists or not. If group is not there, create it first
    group_find
    r=$?
    if [ "$r" != "0" ]; then
        echo "Group $GROUPNAME not found, adding group"

        out="$(ipa group-add $GROUPNAME)"
        r=$?
        [[ "$r" != "0" ]] && echo "Error while adding group $GROUPNAME" && return 1;

        echo "Group $GROUPNAME Added"
    fi


    err="$(ipa group-add-member $GROUPNAME --users=$USERNAME 2>&1)"
    r=$?
    [[ "$r" != "0" ]] && echo "Error while assigning group $GROUPNAME for user $USERNAME $err" && return 1;

    echo "User's group updated"
    return 0

}



