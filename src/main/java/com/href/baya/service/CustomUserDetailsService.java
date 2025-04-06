package com.href.baya.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.href.baya.model.BayaUser;
import com.href.baya.repository.BayaUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service // By annotating the class with @Service,
         // it becomes part of the Spring application context. This allows it to
         // be automatically discovered and injected
          //into other components
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private BayaUserRepository userRepository;

    @Override

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        BayaUser user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        System.out.println("Roles for " + username + ": " + user.getRoles());
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream().map(SimpleGrantedAuthority::new).toList())
                .build();
    }
}