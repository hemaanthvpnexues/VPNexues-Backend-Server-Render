package com.vpnexues.svc.service;

import com.vpnexues.svc.dto.B2BEnquiryDto;
import com.vpnexues.svc.dto.CreateB2BEnquiryRequest;
import com.vpnexues.svc.entity.B2BEnquiry;
import com.vpnexues.svc.entity.B2BEnquiryStatus;
import com.vpnexues.svc.exception.NotFoundException;
import com.vpnexues.svc.repository.B2BEnquiryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class B2BEnquiryService {

    private final B2BEnquiryRepository b2bEnquiryRepository;
    private final B2BEnquiryMailService mailService;

    public B2BEnquiryDto create(CreateB2BEnquiryRequest req) {
        B2BEnquiry enquiry = new B2BEnquiry();
        enquiry.setCompanyName(req.companyName());
        enquiry.setContactName(req.contactName());
        enquiry.setEmail(req.email());
        enquiry.setPhone(req.phone());
        enquiry.setCountryCode(req.countryCode());
        enquiry.setProductInterest(req.productInterest());
        enquiry.setQuantity(req.quantity());
        enquiry.setEstimatedValue(req.estimatedValue());
        B2BEnquiry saved = b2bEnquiryRepository.save(enquiry);
        // Confirmation to the customer + notification copy to the team inbox.
        // Both methods swallow their own errors — a mail failure never fails the save.
        mailService.sendCustomerConfirmation(saved);
        mailService.sendTeamNotification(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<B2BEnquiryDto> list(B2BEnquiryStatus status) {
        List<B2BEnquiry> enquiries = status == null
                ? b2bEnquiryRepository.findAllByOrderByCreatedAtDesc()
                : b2bEnquiryRepository.findByStatusOrderByCreatedAtDesc(status);
        return enquiries.stream().map(this::toDto).toList();
    }

    public B2BEnquiryDto updateStatus(UUID id, B2BEnquiryStatus status) {
        B2BEnquiry enquiry = b2bEnquiryRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("B2B enquiry not found: " + id));
        enquiry.setStatus(status);
        return toDto(b2bEnquiryRepository.save(enquiry));
    }

    private B2BEnquiryDto toDto(B2BEnquiry e) {
        return new B2BEnquiryDto(
                e.getId(),
                e.getCompanyName(),
                e.getContactName(),
                e.getEmail(),
                e.getPhone(),
                e.getCountryCode(),
                e.getProductInterest(),
                e.getQuantity(),
                e.getEstimatedValue(),
                e.getStatus().name(),
                e.getCreatedAt());
    }
}
