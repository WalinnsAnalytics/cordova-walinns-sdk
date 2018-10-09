package com.walinns.hybrid;

/**
 * Created by walinnsinnovation on 25/07/18.
 */

public class UserProfile {
    String first_name;
    String last_name;
    String email;
    String phone_no;

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPhone_no(String phone_no) {
        this.phone_no = phone_no;
    }

    public String getPhone_no() {
        return phone_no;
    }
}
