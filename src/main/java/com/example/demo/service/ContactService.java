package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.repository.ContactRepository;
import com.example.demo.model.Contact;
import com.example.demo.model.LinkPrecedence;
import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    public IdentifyResponse identify(IdentifyRequest request) {

        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        List<Contact> matchedContacts = new ArrayList<>();

        if (email != null) {
            matchedContacts.addAll(contactRepository.findByEmail(email));
        }

        if (phone != null) {
            matchedContacts.addAll(contactRepository.findByPhoneNumber(phone));
        }

        // Remove duplicates
        matchedContacts = matchedContacts.stream().distinct().collect(Collectors.toList());

        if (matchedContacts.isEmpty()) {
            // Create new primary contact
            Contact newContact = new Contact();
            newContact.setEmail(email);
            newContact.setPhoneNumber(phone);
            newContact.setLinkPrecedence(LinkPrecedence.PRIMARY);
            newContact.setLinkedId(null);

            Contact saved = contactRepository.save(newContact);

            return buildResponse(saved, new ArrayList<>());
        }

        // Get oldest contact as primary
        Contact primary = matchedContacts.stream()
                .min(Comparator.comparing(Contact::getCreatedAt))
                .get();

        // Convert other primaries to secondary if needed
        for (Contact contact : matchedContacts) {
            if (!contact.getId().equals(primary.getId())
                    && contact.getLinkPrecedence() == LinkPrecedence.PRIMARY) {

                contact.setLinkPrecedence(LinkPrecedence.SECONDARY);
                contact.setLinkedId(primary.getId());
                contactRepository.save(contact);
            }
        }

        // If new information exists, create secondary
        boolean emailExists = matchedContacts.stream()
                .anyMatch(c -> email != null && email.equals(c.getEmail()));

        boolean phoneExists = matchedContacts.stream()
                .anyMatch(c -> phone != null && phone.equals(c.getPhoneNumber()));

        if ((!emailExists && email != null) || (!phoneExists && phone != null)) {
            Contact secondary = new Contact();
            secondary.setEmail(email);
            secondary.setPhoneNumber(phone);
            secondary.setLinkPrecedence(LinkPrecedence.SECONDARY);
            secondary.setLinkedId(primary.getId());

            contactRepository.save(secondary);
        }

        List<Contact> allLinked = contactRepository.findAll().stream()
                .filter(c -> c.getId().equals(primary.getId())
                        || (c.getLinkedId() != null && c.getLinkedId().equals(primary.getId())))
                .collect(Collectors.toList());

        return buildResponse(primary, allLinked);
    }

    private IdentifyResponse buildResponse(Contact primary, List<Contact> contacts) {

        if (contacts.isEmpty()) {
            contacts = List.of(primary);
        }

        List<String> emails = contacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> phones = contacts.stream()
                .map(Contact::getPhoneNumber)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Long> secondaryIds = contacts.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                .map(Contact::getId)
                .collect(Collectors.toList());

        IdentifyResponse.ContactResponse response =
                new IdentifyResponse.ContactResponse(
                        primary.getId(),
                        emails,
                        phones,
                        secondaryIds
                );

        return new IdentifyResponse(response);
    }
}