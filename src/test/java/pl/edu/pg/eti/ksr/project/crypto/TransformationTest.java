package pl.edu.pg.eti.ksr.project.crypto;

import org.junit.Assert;
import org.junit.Test;

public class TransformationTest {

    @Test
    public void Should_ReturnProperAlgorithmName_When_GetAlgorithmInvoked() {
        Transformation transformation = Transformation.AES_CBC_NoPadding;
        Assert.assertEquals("AES", transformation.getAlgorithm());
    }
}
