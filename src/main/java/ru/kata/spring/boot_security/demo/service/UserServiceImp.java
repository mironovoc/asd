package ru.kata.spring.boot_security.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;
import ru.kata.spring.boot_security.demo.repository.UserRepository;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.util.*;

@Transactional
@Service
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImp(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User insertUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public User updateUser(User user) {
        if (user.getPassword().isEmpty()) {
            userRepository.findById(user.getId()).ifPresent(userFromDb -> user.setPassword(userFromDb.getPassword()));
        } else {
            user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public User findUserByName(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElse(new User());
    }

    @Override
    public List<User> allUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        }
    }

    @Override
    public List<Role> listRoles() {
        return roleRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        for (Role role : user.getRoles()) {
            grantedAuthorities.add(new SimpleGrantedAuthority(role.getName()));
        }
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), grantedAuthorities);
    }

    @Override
    public Iterable<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public String getPage(Model model, HttpSession session, Authentication auth) {
        if (Objects.isNull(auth)) {
            handleUnauthenticatedUser(model, session);
            return "login-page";
        }
        return handleAuthenticatedUser(model, auth);
    }

    private void handleUnauthenticatedUser(Model model, HttpSession session) {
        model.addAttribute("authenticatedName", session.getAttribute("authenticatedName"));
        session.removeAttribute("authenticatedName");
        model.addAttribute("authenticationException", session.getAttribute("authenticationException"));
        session.removeAttribute("authenticationException");
    }

    private String handleAuthenticatedUser(Model model, Authentication auth) {
        org.springframework.security.core.userdetails.User securityUser =
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        // Получаем вашего пользователя из базы данных
        User user = userRepository.findByUsername(securityUser.getUsername());

        model.addAttribute("user", user);

        if (user.hasRole(1)) {
            return "main-page";
        }
        if (user.hasRole(2)) {
            return "user-page";
        }
        return "access-denied-page";
    }

}