package pl.edu.pg.eti.ksr.project.crypto;

import org.junit.Assert;
import org.junit.Test;

public class TransformationTest {

    @Test
    public void Should_ReturnProperAlgorithmName_When_GetAlgorithmMethodCalled() {
        Transformation transformation = Transformation.AES_CBC_NoPadding;
        Assert.assertEquals("AES", transformation.getAlgorithm());
    }

    @Test
    public void Should_ReturnProperTransformationFromTextRepresentation_When_FromTextMethodCalled() {
        Transformation test = Transformation.AES_CBC_NoPadding;

        Transformation result = Transformation.fromText(test.getText());

        Assert.assertEquals(test, result);
    }
}
