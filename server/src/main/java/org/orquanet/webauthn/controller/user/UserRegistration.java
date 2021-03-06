package org.orquanet.webauthn.controller.user;

import org.orquanet.webauthn.controller.user.dto.UserDto;
import org.orquanet.webauthn.repository.UserRepository;
import org.orquanet.webauthn.repository.model.FidoUser;
import org.orquanet.webauthn.repository.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;

@RestController
public class UserRegistration {

    private SecureRandom secureRandom = new SecureRandom();
    private UserRepository userRepository;

    public UserRegistration(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @CrossOrigin(origins = "${webauthn.origins.allowed}", allowCredentials = "true", methods = {RequestMethod.POST})
    @PostMapping("/registration/user")
    @ResponseStatus(value = HttpStatus.CREATED)
    public void register(@RequestBody UserDto userDTO) {
        byte[] randomBytes = new byte[30];
        User user = User.builder().email(userDTO.getEmail()).firstName(userDTO.getFirstName()).lastName(userDTO.getLastName()).build();
        secureRandom.nextBytes(randomBytes);
        FidoUser fidoUser = FidoUser.builder().user(user).fidoId(Base64.getEncoder().encodeToString(randomBytes)).build();
        this.userRepository.save(fidoUser);
    }

}
