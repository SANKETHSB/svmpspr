package com.infosys.svpms.utility;

import com.infosys.svpms.entity.User;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final VendorRepository vendorRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        seedUser("System Admin", "admin@svpms.com", "Admin@123456", User.Role.ADMIN);
        seedUser("Procurement Manager", "manager@svpms.com", "Manager@123456", User.Role.PROCUREMENT_MANAGER);
        seedUser("Compliance Officer", "compliance@svpms.com", "Compliance@123456", User.Role.COMPLIANCE_OFFICER);
        seedVendor("TechSupply Pvt Ltd", "vendor@techsupply.com", "Vendor@123456", "27AAPFU0939F1ZV", "CIN-U12345MH2024");
        log.info("=== SVPMS Data Seeded. Login: admin@svpms.com / Admin@123456 ===");
    }

    private void seedUser(String name, String email, String password, User.Role role) {
        if (!userRepo.existsByEmail(email)) {
            userRepo.save(User.builder()
                .name(name).email(email).password(encoder.encode(password))
                .role(role).active(true).build());
            log.info("Seeded user: {}", email);
        }
    }

    private void seedVendor(String company, String email, String password, String gst, String regId) {
        if (!vendorRepo.existsByEmail(email)) {
            vendorRepo.save(Vendor.builder()
                .companyName(company).email(email).password(encoder.encode(password))
                .gstNumber(gst).registrationId(regId)
                .phone("9876543210").address("123 Tech Park, Mumbai")
                .contactPerson("Rajesh Kumar")
                .status(Vendor.VendorStatus.APPROVED)
                .compliant(true).build());
            log.info("Seeded vendor: {}", email);
        }
    }
}
