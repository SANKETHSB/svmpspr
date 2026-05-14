package com.infosys.svpms.security;

import com.infosys.svpms.entity.User;
import com.infosys.svpms.entity.Vendor;
import com.infosys.svpms.repository.UserRepository;
import com.infosys.svpms.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CombinedUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try internal user first
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            // AC #4: If user has a custom role assigned, use it as the authority
            String authority = (u.getCustomRoleName() != null && !u.getCustomRoleName().isBlank())
                ? "ROLE_" + u.getCustomRoleName()
                : "ROLE_" + u.getRole().name();
            return org.springframework.security.core.userdetails.User.builder()
                    .username(u.getEmail())
                    .password(u.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority(authority)))
                    .accountLocked(u.isLocked())
                    .disabled(!u.isActive())
                    .build();
        }
        // Try vendor
        var vendorOpt = vendorRepository.findByEmail(email);
        if (vendorOpt.isPresent()) {
            Vendor v = vendorOpt.get();
            
            // Check if vendor is approved
            if (v.getStatus() != Vendor.VendorStatus.APPROVED) {
                throw new org.springframework.security.authentication.DisabledException(
                    "Vendor account is not approved. Status: " + v.getStatus()
                );
            }
            
            return org.springframework.security.core.userdetails.User.builder()
                    .username(v.getEmail())
                    .password(v.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_VENDOR")))
                    .accountLocked(v.isLocked())
                    .disabled(false)
                    .build();
        }
        throw new UsernameNotFoundException("User not found: " + email);
    }
}
