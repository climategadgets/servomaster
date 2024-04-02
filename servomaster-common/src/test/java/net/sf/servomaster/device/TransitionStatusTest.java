package net.sf.servomaster.device;

import net.sf.servomaster.device.model.TransitionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TransitionStatusTest {

    @Test
    void testAuth() {

        TransitionStatus target = new TransitionStatus(0);

        assertThatExceptionOfType(IllegalAccessError.class)
                .isThrownBy(() -> target.complete(1, null))
                .withMessage("invalid token, refusing to set status");
    }

    @Test
    void testCompletion() {

        TransitionStatus target = new TransitionStatus(0);

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(target::isOK)
                .withMessage("haven't completed yet");
    }

    @Test
    void testPass() {

        TransitionStatus target = new TransitionStatus(0);

        target.complete(0, null);

        assertThat(target.isOK()).isTrue();
        assertThat(target.getCause()).isNull();
    }

    @Test
    void testFail() {

        TransitionStatus target = new TransitionStatus(0);
        Throwable t = new IllegalStateException();

        target.complete(0, t);

        assertThat(target.isOK()).isFalse();
        assertThat(target.getCause()).isEqualTo(t);
    }
}
