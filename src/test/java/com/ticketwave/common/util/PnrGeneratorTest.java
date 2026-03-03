package com.ticketwave.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PnrGeneratorTest {
    private final PnrGenerator pnrGenerator = new PnrGenerator();

    @Test
    void generateValidPnr() {
        String pnr = pnrGenerator.generate();
        assertNotNull(pnr);
        assertTrue(pnr.length() > 0);
        // PNR should be alphanumeric
        assertTrue(pnr.matches("[A-Z0-9]+"));
    }

    @Test
    void generateUniquePnrs() {
        String pnr1 = pnrGenerator.generate();
        String pnr2 = pnrGenerator.generate();
        assertNotEquals(pnr1, pnr2);
    }

    @Test
    void generateedPnrFormat() {
        String pnr = pnrGenerator.generate();
        // Typical PNR format: 6 alphanumeric characters
        assertTrue(pnr.length() >= 6);
    }
}
