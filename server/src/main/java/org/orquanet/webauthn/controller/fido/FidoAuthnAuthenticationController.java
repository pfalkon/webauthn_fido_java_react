/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orquanet.webauthn.controller.fido;

import org.orquanet.webauthn.controller.session.WebauthnSession;
import org.orquanet.webauthn.controller.user.dto.UserDto;
import org.orquanet.webauthn.crypto.KeyInfo;
import org.orquanet.webauthn.repository.model.FidoCredential;
import org.orquanet.webauthn.repository.model.FidoUser;
import org.orquanet.webauthn.service.CredentialService;
import org.orquanet.webauthn.service.UserService;
import org.orquanet.webauthn.webauthn.assertion.data.AuthenticatorAssertion;
import org.orquanet.webauthn.webauthn.assertion.data.PublicKeyCredentialRequestOptions;
import org.orquanet.webauthn.webauthn.assertion.exception.AuthenticationException;
import org.orquanet.webauthn.webauthn.assertion.reader.AuthenticatorAssertionReader;
import org.orquanet.webauthn.webauthn.assertion.validation.clientdata.ClientDataAuthenticationValidation;
import org.orquanet.webauthn.webauthn.assertion.validation.signature.AssertionSignatureVerifier;
import org.orquanet.webauthn.webauthn.attestation.exception.RegistrationException;
import org.orquanet.webauthn.webauthn.common.data.PublicKeyCredentialDescriptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class FidoAuthnAuthenticationController  extends FidoController {

    private AssertionSignatureVerifier assertionSignatureVerifier;
    private UserService userService;
    private CredentialService credentialService;
    private AuthenticatorAssertionReader authenticatorAssertionReader;
    private ClientDataAuthenticationValidation clientDataAuthenticationValidation;

    public final String AUTHENTICATION_SESSION_NAME = "authentication_session";

    public FidoAuthnAuthenticationController(AssertionSignatureVerifier assertionSignatureVerifier,
                                             AuthenticatorAssertionReader authenticatorAssertionReader,
                                             UserService userService,
                                             CredentialService credentialService,
                                             ClientDataAuthenticationValidation clientDataAuthenticationValidation){
        this.authenticatorAssertionReader = authenticatorAssertionReader;
        this.assertionSignatureVerifier = assertionSignatureVerifier;
        this.userService = userService;
        this.credentialService = credentialService;
        this.clientDataAuthenticationValidation = clientDataAuthenticationValidation;
       // this.clientDataValidation = clientDataValidation;
    }

    @CrossOrigin(origins = "${webauthn.origins.allowed}", allowCredentials = "true", methods = {RequestMethod.POST})
    @PostMapping(value = "/authenticate/init",consumes = MediaType.APPLICATION_JSON_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public PublicKeyCredentialRequestOptions authenticateInit(@RequestBody UserDto userDto,HttpServletRequest request){
        String email = userDto.getEmail();
        Optional<FidoUser> fidoUserOptional = userService.findUser(email);
        FidoUser fidoUser = fidoUserOptional.orElseThrow(AuthenticationException::new);

        Set<PublicKeyCredentialDescriptor> allowCredentials = fidoUser
                                    .getFidoCredentials()
                                    .stream()
                                     .map(c -> PublicKeyCredentialDescriptor
                                             .builder()
                                             .id(c.getCredentialId()).type("public-key").build())
                                             .collect(Collectors.toSet());

        String challenge = Base64.getEncoder().encodeToString(challenge());

        initWebauthnSession(AUTHENTICATION_SESSION_NAME,request,challenge, fidoUser);

        return PublicKeyCredentialRequestOptions
                .builder()
                .challenge(challenge)
                .timeout(java.util.Optional.of(60000))
                .allowCredentials(allowCredentials)
                .build();
    }


    @CrossOrigin(origins = "${webauthn.origins.allowed}", allowCredentials = "true", methods = {RequestMethod.POST})
    @PostMapping(value = "/authenticate/final",produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void authenticateFinal(@RequestBody AuthenticatorAssertion authenticatorAssertion, HttpServletRequest request){
        //authenticatorAssertion.getResponse().getUserHandle()
        HttpSession session = request.getSession();
        WebauthnSession webauthnSession = (WebauthnSession) session.getAttribute(AUTHENTICATION_SESSION_NAME);
        session.invalidate();

        FidoUser fidoUser = webauthnSession.getFidoUser();
        String fidoId = fidoUser.getFidoId();

        authenticatorAssertionReader.readAuthData(authenticatorAssertion);
        String credentialId = authenticatorAssertion.getRawId();
        Optional<FidoCredential> fidoCredentialOptional = credentialService.credential(credentialId,fidoId);
        FidoCredential fidoCredential = fidoCredentialOptional.orElseThrow(RegistrationException::new);

        byte[] publicKey = fidoCredential.getPublicKey();

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA","BC");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
            PublicKey pk = keyFactory.generatePublic(publicKeySpec);
            KeyInfo keyInfo = KeyInfo.builder().publicKey(pk).build();
            assertionSignatureVerifier.verify(authenticatorAssertion, keyInfo);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void initAuthenticationSession(HttpServletRequest request, String challenge, FidoUser fidoUser) {
        WebauthnSession webauthnSession = new WebauthnSession();
        webauthnSession.setChallenge(challenge);
        webauthnSession.setFidoUser(fidoUser);
        HttpSession session = request.getSession(true);
        session.setAttribute(AUTHENTICATION_SESSION_NAME, webauthnSession);
    }

}
