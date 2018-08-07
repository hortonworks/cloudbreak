package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class ImageCatalog extends ImageCatalogEntity {

    static Function<IntegrationTestContext, ImageCatalog> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, ImageCatalog.class);
    }

    static Function<IntegrationTestContext, ImageCatalog> getNew() {
        return testContext -> new ImageCatalog();
    }

    public static ImageCatalog request() {
        return new ImageCatalog();
    }

    public static ImageCatalog isCreated() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setCreationStrategy(ImageCatalogAction::createInGiven);
        return imageCatalog;
    }

    public static ImageCatalog isCreatedDeleted() {
        ImageCatalog  imageCatalog = new ImageCatalog();
        imageCatalog.setCreationStrategy(ImageCatalogAction::createDeleteInGiven);
        return imageCatalog;
    }

    public static ImageCatalog isCreatedAsDefault() {
        ImageCatalog  imageCatalog = new ImageCatalog();
        imageCatalog.setCreationStrategy(ImageCatalogAction::createAsDefaultInGiven);
        return imageCatalog;
    }

    public static Action<ImageCatalog> post(String key) {
        return new Action<>(getTestContext(key), ImageCatalogAction::post);
    }

    public static Action<ImageCatalog> post() {
        return post(IMAGE_CATALOG);
    }

    public static Action<ImageCatalog> get(String key) {
        return new Action<>(getTestContext(key), ImageCatalogAction::get);
    }

    public static Action<ImageCatalog> get() {
        return get(IMAGE_CATALOG);
    }

    public static Action<ImageCatalog> getAll() {
        return new Action<>(getNew(), ImageCatalogAction::getAll);
    }

    public static Action<ImageCatalog> getImagesByProvider() {
        return new Action<>(getNew(), ImageCatalogAction::getImagesByProvider); }

    public static Action<ImageCatalog> getImagesByProviderFromImageCatalog(String key) {
        return new Action<>(getTestContext(key),  ImageCatalogAction::getImagesByProviderFromImageCatalog); }

    public static Action<ImageCatalog> getImagesByProviderFromImageCatalog() {
        return getImagesByProviderFromImageCatalog(IMAGE_CATALOG); }

    public static Action<ImageCatalog> getRequestFromName(String key) {
        return new Action<>(getTestContext(key), ImageCatalogAction::getRequestByName); }

    public static Action<ImageCatalog> getRequestFromName() {
        return getRequestFromName(IMAGE_CATALOG); }

    public static Action<ImageCatalog> delete(String key) {
        return new Action<>(getTestContext(key), ImageCatalogAction::delete);
    }

    public static Action<ImageCatalog> delete() {
        return delete(IMAGE_CATALOG);
    }

    public static Assertion<ImageCatalog> assertThis(BiConsumer<ImageCatalog, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Action<ImageCatalog> setDefault(String key) {
        return new Action<>(getTestContext(key), ImageCatalogAction::putSetDefaultByName);
    }

    public static Action<ImageCatalog> setDefault() {
        return setDefault(IMAGE_CATALOG);
    }
}