package com.href.baya.controller;
import com.href.baya.model.BayaUser;
import com.href.baya.repository.BayaUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

/*
Purpose: Handles HTTP requests and returns views or redirects.
*/

@Controller //Marks this as a Spring MVC controller.
public class BayaUserController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private BayaUserRepository userRepository;
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/"; // Directory for uploaded images
    private static final Logger logger = LoggerFactory.getLogger(BayaUserController.class);
    @GetMapping("/")
    public String home()
    {
        return "home";//: Maps GET / to the home.html template (accessible after login).
    }

    @GetMapping("/login")
    public String login()
    {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new BayaUser());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute BayaUser user, Model model) {
        BayaUser existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            model.addAttribute("errorMessage", "Username already exists!");
            return "register";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.addRole("ROLE_USER");
        if ("admin".equals(user.getUsername())) {
            user.addRole("ROLE_ADMIN");
        }
        userRepository.save(user);
        return "redirect:/login";
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "logout";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        String username = getCurrentUsername();
        model.addAttribute("username", username);
        return "admin/dashboard";
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Model model) {
        String username = getCurrentUsername();
        model.addAttribute("username", username);
        return "user/dashboard";
    }
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = getCurrentUsername();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication()
                        .getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        model.addAttribute("username", username != null ? username : "Guest");
        model.addAttribute("isAdmin", isAdmin);
        return "dashboard";
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return principal.toString();
            }
        }
        return null;


    }

    // Show profile edit form
    @GetMapping("/profile/edit")
    public String showProfileEditForm(Model model) {
        BayaUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Redirect to login if not authenticated
        }
        model.addAttribute("user", currentUser);
        return "profile-edit"; // Thymeleaf template name
    }

    // Process profile edit form submission

    @PostMapping("/profile/edit")
    public String updateProfile(
            @ModelAttribute BayaUser updatedUser,
            @RequestParam("image") MultipartFile image, // Add file upload parameter
            Model model,
            HttpServletRequest request) {
        logger.debug("Handling POST request to /profile/edit");
        logger.debug("Form params: {}", Collections.list(request.getParameterNames())
                .stream().collect(Collectors.toMap(name -> name, request::getParameter)));

        BayaUser currentUser = getCurrentUser();
        if (currentUser == null) {
            logger.warn("No authenticated user, redirecting to login");
            return "redirect:/login";
        }
        logger.debug("Current user: {}", currentUser.getUsername());

        try {
            // Handle email update
            if (updatedUser.getEmail() != null && !updatedUser.getEmail().equals(currentUser.getEmail())) {
                BayaUser existing = userRepository.findByEmail(updatedUser.getEmail());
                if (existing != null && !existing.getId().equals(currentUser.getId())) {
                    logger.warn("Email {} already in use", updatedUser.getEmail());
                    model.addAttribute("errorMessage", "Email already in use!");
                    model.addAttribute("user", currentUser);
                    return "profile-edit";
                }
                currentUser.setEmail(updatedUser.getEmail());
            }

            // Handle password update
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                logger.debug("Encoding new password for user: {}", currentUser.getUsername());
                currentUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            // Handle image upload
            if (!image.isEmpty()) {
                String fileName = currentUser.getUsername() + "_" + image.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(path.getParent()); // Ensure directory exists
                Files.write(path, image.getBytes());
                currentUser.setProfileImage("/uploads/" + fileName); // Store relative path
                logger.debug("Profile image uploaded: {}", currentUser.getProfileImage());
            }

            logger.debug("Saving user to database");
            userRepository.save(currentUser);
            logger.debug("User saved successfully");
            model.addAttribute("successMessage", "Profile updated successfully!");
            model.addAttribute("user", currentUser);
            return "profile-edit";
        } catch (IOException e) {
            logger.error("Error uploading image: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to upload image: " + e.getMessage());
            model.addAttribute("user", currentUser);
            return "profile-edit";
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
            model.addAttribute("user", currentUser);
            return "profile-edit";
        }
    }

    // Helper method to get current authenticated user
    private BayaUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                String username = ((UserDetails) principal).getUsername();
                BayaUser user = userRepository.findByUsername(username);
                if (user == null) {
                    System.out.println("No user found for username: " + username);
                } else {
                    System.out.println("Found user: " + user.getUsername());
                }
                return user;
            }
        }
        System.out.println("No authenticated user found");
        return null;
    }
}