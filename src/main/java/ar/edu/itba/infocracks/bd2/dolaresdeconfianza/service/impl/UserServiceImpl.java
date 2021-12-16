package ar.edu.itba.infocracks.bd2.dolaresdeconfianza.service.impl;

import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.config.GeometryConfig;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.controller.dto.UserDTO;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.exception.InvalidCredentialsException;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.model.neo4j.UserNode;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.model.postgres.UserEntity;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.repository.postgres.UserEntityRepository;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.repository.neo4j.UserNodeRepository;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.security.AuthenticationResponse;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.security.LoginForm;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.security.SessionUtils;
import ar.edu.itba.infocracks.bd2.dolaresdeconfianza.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.transaction.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserEntityRepository userEntityRepository;
    private final UserNodeRepository userNodeRepository;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SessionUtils sessionUtils;

    private final PasswordEncoder encoder;

    @Autowired
    public UserServiceImpl(PasswordEncoder passwordEncoder, UserEntityRepository userEntityRepository, UserNodeRepository userNodeRepository, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, SessionUtils sessionUtils){
        this.encoder = passwordEncoder;
        this.userEntityRepository = userEntityRepository;
        this.userNodeRepository = userNodeRepository;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.sessionUtils = sessionUtils;
    }

    @Override
    @Transactional
    public UserEntity save(String username, String email, String password, String firstName, String lastName, double locationX, double locationY) {

        UserEntity user = userEntityRepository.save(new UserEntity(username, email, encoder.encode(password), firstName,lastName, GeometryConfig.pointFromCoordinates(locationX,locationY)));
        UserNode userNode = userNodeRepository.save(new UserNode(user.getId(),user.getUsername()));

        LOGGER.info("Saved {} to both databases", user);
        return user;
    }

    @Override
    public AuthenticationResponse authenticate(LoginForm authenticationRequest) throws InvalidCredentialsException {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(token);
        }
        catch (AuthenticationException e){
            throw new InvalidCredentialsException();
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);


        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String jwt = sessionUtils.generateToken(userDetails);

        return new AuthenticationResponse(jwt);
    }

    public UserDTO signUp(UserDTO user) {
        UserEntity userEntity = this.save(user.getUsername(), user.getEmail(), user.getPassword(), user.getFirstName(), user.getLastName(), user.getLocationX(), user.getLocationY());
        return new UserDTO(userEntity.getId(), userEntity.getUsername(), userEntity.getEmail(), userEntity.getFirstName(), userEntity.getLastName(), userEntity.getLocation().getX(),userEntity.getLocation().getY());
    }



}
