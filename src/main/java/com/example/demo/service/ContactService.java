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

     // Fetch all contacts related to primary
        List<Contact> allLinked = new ArrayList<>();

        // Add primary
        allLinked.add(primary);

        // Add all secondaries linked to primary
        allLinked.addAll(contactRepository.findByLinkedId(primary.getId()));

        return buildResponse(primary, allLinked);
    }

    private IdentifyResponse buildResponse(Contact primary, List<Contact> contacts) {

        Set<String> emailSet = new LinkedHashSet<>();
        Set<String> phoneSet = new LinkedHashSet<>();
        List<Long> secondaryIds = new ArrayList<>();

        // Add primary first
        if (primary.getEmail() != null) emailSet.add(primary.getEmail());
        if (primary.getPhoneNumber() != null) phoneSet.add(primary.getPhoneNumber());

        for (Contact c : contacts) {
            if (!c.getId().equals(primary.getId())) {
                if (c.getEmail() != null) emailSet.add(c.getEmail());
                if (c.getPhoneNumber() != null) phoneSet.add(c.getPhoneNumber());
                if (c.getLinkPrecedence() == LinkPrecedence.SECONDARY)
                    secondaryIds.add(c.getId());
            }
        }

        IdentifyResponse.ContactResponse response =
                new IdentifyResponse.ContactResponse(
                        primary.getId(),
                        new ArrayList<>(emailSet),
                        new ArrayList<>(phoneSet),
                        secondaryIds
                );

        return new IdentifyResponse(response);
    }
}