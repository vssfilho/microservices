package academy.devdojo.youtube.security.creator;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import academy.devdojo.youtube.core.model.ApplicationUser;
import academy.devdojo.youtube.core.property.JwtConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TokenCreator {

	private final JwtConfiguration jwtConfiguration;

	@SneakyThrows
	public SignedJWT createSignedJWT(Authentication auth) {
		log.info("Starting to create the signed JWT");
		ApplicationUser applicationUser = (ApplicationUser) auth.getPrincipal();
		JWTClaimsSet jwtClaimsSet = createJWTClaimsSet(auth, applicationUser);
		KeyPair rsaKeys = generateKeyPair();
		log.info("Building JWK from RSA Keys");
		JWK jwk = new RSAKey.Builder((RSAPublicKey) rsaKeys.getPublic()).keyID(UUID.randomUUID().toString()).build();
		SignedJWT signedJWT = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).jwk(jwk).type(JOSEObjectType.JWT).build(), jwtClaimsSet);
		log.info("Signing the token with the private RSA Key");
		RSASSASigner signer = new RSASSASigner(rsaKeys.getPrivate());
		signedJWT.sign(signer);
		log.info("Serialized token '{}'", signedJWT.serialize());
		return signedJWT;
	}

	private JWTClaimsSet createJWTClaimsSet(Authentication auth, ApplicationUser applicationUser) {
		log.info("Creating the JwtClaimSet Object for '{}'", applicationUser);
		return new JWTClaimsSet.Builder().subject(applicationUser.getUsername())
				.claim("authorities",
						auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
				.claim("userId", applicationUser.getId())
				.issuer("http://academy.devdojo").issueTime(new Date())
				.expirationTime(new Date(System.currentTimeMillis() + (jwtConfiguration.getExpiration() * 1000)))
				.build();
	}

	@SneakyThrows
	private KeyPair generateKeyPair() {
		log.info("Generating 2048 bits Keys");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.genKeyPair();
	}

	public String encryptToken(SignedJWT signedJWT) throws JOSEException {
		log.info("Starting the encryptToken method");
		DirectEncrypter directEncrypter = new DirectEncrypter(jwtConfiguration.getPrivateKey().getBytes());
		JWEObject jweObject = new JWEObject(
				new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256).contentType("JWT").build(),
				new Payload(signedJWT));
		log.info("Encrypting token with system's private key");
		jweObject.encrypt(directEncrypter);
		log.info("Token encrypted");
		return jweObject.serialize();
	}

}
