package com.vahanalerts.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegistrationForm form) {
        String phoneNumber = PhoneNumber.normalize(form.getPhoneNumber());
        if (userAccountRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneNumberAlreadyRegisteredException();
        }

        UserAccount user = new UserAccount(
                form.getName().trim(),
                phoneNumber,
                form.getDateOfBirth(),
                passwordEncoder.encode(form.getPassword()),
                form.isWhatsappReminderConsent()
        );
        try {
            userAccountRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isPhoneNumberUniqueConstraintViolation(exception)) {
                throw new PhoneNumberAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    private boolean isPhoneNumberUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_app_user_phone_number".equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
