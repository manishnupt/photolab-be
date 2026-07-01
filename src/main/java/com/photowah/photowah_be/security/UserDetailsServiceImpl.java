package com.photowah.photowah_be.security;

import com.photowah.photowah_be.repository.PhotographerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PhotographerRepository photographerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return photographerRepository.findByEmail(email)
                .map(p -> new User(
                        p.getEmail(),
                        p.getPasswordHash(),
                        List.of(new SimpleGrantedAuthority("ROLE_PHOTOGRAPHER"))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Photographer not found: " + email));
    }
}
