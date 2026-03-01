package com.example.demo.dto;

import java.util.List;

public class IdentifyResponse {

    private ContactResponse contact;

    public IdentifyResponse(ContactResponse contact) {
        this.contact = contact;
    }

    public ContactResponse getContact() {
        return contact;
    }

    public void setContact(ContactResponse contact) {
        this.contact = contact;
    }

    public static class ContactResponse {

        private Long primaryContatctId;
        private List<String> emails;
        private List<String> phoneNumbers;
        private List<Long> secondaryContactIds;

        public ContactResponse(Long primaryContatctId,
                               List<String> emails,
                               List<String> phoneNumbers,
                               List<Long> secondaryContactIds) {
            this.primaryContatctId = primaryContatctId;
            this.emails = emails;
            this.phoneNumbers = phoneNumbers;
            this.secondaryContactIds = secondaryContactIds;
        }

        public Long getPrimaryContatctId() {
            return primaryContatctId;
        }

        public List<String> getEmails() {
            return emails;
        }

        public List<String> getPhoneNumbers() {
            return phoneNumbers;
        }

        public List<Long> getSecondaryContactIds() {
            return secondaryContactIds;
        }
    }
}