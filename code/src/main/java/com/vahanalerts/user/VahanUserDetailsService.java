package com.vahanalerts.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VahanUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public VahanUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String phoneNumber) {
        String normalizedPhoneNumber = PhoneNumber.normalize(phoneNumber);
        if (normalizedPhoneNumber == null || !normalizedPhoneNumber.startsWith("+")) {
            throw new UsernameNotFoundException("User not found");
        }
        UserAccount user = userAccountRepository.findByPhoneNumber(normalizedPhoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new VahanUserDetails(user);
    }
}
