/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.keycloak.common.crypto;

import java.net.URI;
import java.security.cert.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/29/2016
 */

public abstract class OCSPUtils {

    private final static Logger logger = Logger.getLogger(""+OCSPUtils.class);

    protected static int OCSP_CONNECT_TIMEOUT = 10000; // 10 sec
    protected static final int TIME_SKEW = 900000;

    public enum RevocationStatus {
        GOOD,
        REVOKED,
        UNKNOWN
    }

    public interface OCSPRevocationStatus {
        RevocationStatus getRevocationStatus();
        Date getRevocationTime();
        CRLReason getRevocationReason();
    }

    /**
     * Requests certificate revocation status using OCSP.
     * @param cert the certificate to be checked
     * @param issuerCertificate The issuer certificate
     * @param responderURI an address of OCSP responder. Overrides any OCSP responder URIs stored in certificate's AIA extension
     * @param date
     * @param responderCert a certificate that OCSP responder uses to sign OCSP responses
     * @return revocation status
     */
    public OCSPRevocationStatus check( X509Certificate cert, X509Certificate issuerCertificate, URI responderURI, X509Certificate responderCert, Date date) throws CertPathValidatorException {
        if (cert == null)
            throw new IllegalArgumentException("cert cannot be null");
        if (issuerCertificate == null)
            throw new IllegalArgumentException("issuerCertificate cannot be null");
        if (responderURI == null)
            throw new IllegalArgumentException("responderURI cannot be null");

        return check( cert, issuerCertificate, Collections.singletonList(responderURI), responderCert, date);
    }
    /**
     * Requests certificate revocation status using OCSP. The OCSP responder URI
     * is obtained from the certificate's AIA extension.
     * @param cert the certificate to be checked
     * @param issuerCertificate The issuer certificate
     * @param date
     * @return revocation status
     */
    public OCSPRevocationStatus check( X509Certificate cert, X509Certificate issuerCertificate, Date date, X509Certificate responderCert) throws CertPathValidatorException {
        List<String> responderURIs = null;
        try {
            responderURIs = getResponderURIs(cert);
        } catch (CertificateEncodingException e) {
            logger.log(Level.FINE, "CertificateEncodingException: {0}", e.getMessage());
            throw new CertPathValidatorException(e.getMessage(), e);
        }
        if (responderURIs.size() == 0) {
            logger.log(Level.INFO, "No OCSP responders in the specified certificate");
            throw new CertPathValidatorException("No OCSP Responder URI in certificate");
        }

        List<URI> uris = new LinkedList<>();
        for (String value : responderURIs) {
            try {
                URI responderURI = URI.create(value);
                uris.add(responderURI);
            } catch (IllegalArgumentException ex) {
                logger.log(Level.FINE, "Malformed responder URI {0}", value);
            }
        }
        return check( cert, issuerCertificate, Collections.unmodifiableList(uris), responderCert, date);
    }
    /**
     * Requests certificate revocation status using OCSP. The OCSP responder URI
     * is obtained from the certificate's AIA extension.
     * @param cert the certificate to be checked
     * @param issuerCertificate The issuer certificate
     * @return revocation status
     */
    public OCSPRevocationStatus check( X509Certificate cert, X509Certificate issuerCertificate) throws CertPathValidatorException {
        return check( cert, issuerCertificate, null, null);
    }

    /**
     * Requests certificate revocation status using OCSP.
     * @param cert the certificate to be checked
     * @param issuerCertificate the issuer certificate
     * @param responderURIs the OCSP responder URIs
     * @param responderCert the OCSP responder certificate
     * @param date if null, the current time is used.
     * @return a revocation status
     * @throws CertPathValidatorException
     */
    protected abstract OCSPRevocationStatus check( X509Certificate cert,
            X509Certificate issuerCertificate, List<URI> responderURIs, X509Certificate responderCert, Date date)
            throws CertPathValidatorException;


    protected static OCSPRevocationStatus unknownStatus() {
        return new OCSPRevocationStatus() {
            @Override
            public RevocationStatus getRevocationStatus() {
                return RevocationStatus.UNKNOWN;
            }

            @Override
            public Date getRevocationTime() {
                return new Date(System.currentTimeMillis());
            }

            @Override
            public CRLReason getRevocationReason() {
                return CRLReason.UNSPECIFIED;
            }
        };
    }

    /**
     * Extracts OCSP responder URI from X509 AIA v3 extension, if available. There can be
     * multiple responder URIs encoded in the certificate.
     * @param cert
     * @return a list of available responder URIs.
     * @throws CertificateEncodingException
     */
    protected abstract List<String> getResponderURIs(X509Certificate cert) throws CertificateEncodingException;


}
