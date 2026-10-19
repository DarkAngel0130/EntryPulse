
package org.entrypulse.entrypulse.controller;

import lombok.RequiredArgsConstructor;
import org.entrypulse.entrypulse.service.InvitationService;
import org.entrypulse.entrypulse.service.QRCodeService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr-codes")
@RequiredArgsConstructor
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final InvitationService invitationService;

    @GetMapping(
            value = "/invitations/{invitationId}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> generateInvitationQRCode(
            @PathVariable String invitationId,
            Authentication authentication) {

        String token = invitationService
                .getInvitationTokenForOrganiser(
                        invitationId,
                        authentication.getName());

        byte[] qrCode = qrCodeService.generateQRCode(token);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"invitation-qr.png\"")
                .body(qrCode);
    }
}
