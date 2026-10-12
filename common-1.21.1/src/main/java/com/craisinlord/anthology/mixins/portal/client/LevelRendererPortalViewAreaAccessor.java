package com.craisinlord.anthology.mixins.portal.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererPortalViewAreaAccessor {
    @Accessor("sectionRenderDispatcher")
    SectionRenderDispatcher anthology$getSectionRenderDispatcher();

    @Accessor("viewArea")
    ViewArea anthology$getViewArea();

    @Accessor("viewArea")
    void anthology$setViewArea(ViewArea viewArea);

    @Accessor("sectionOcclusionGraph")
    SectionOcclusionGraph anthology$getSectionOcclusionGraph();

    @Mutable
    @Accessor("sectionOcclusionGraph")
    void anthology$setSectionOcclusionGraph(SectionOcclusionGraph graph);

    @Accessor("visibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> anthology$getVisibleSections();

    @Mutable
    @Accessor("visibleSections")
    void anthology$setVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections);

    @Accessor("globalBlockEntities")
    Set<BlockEntity> anthology$getGlobalBlockEntities();

    @Mutable
    @Accessor("globalBlockEntities")
    void anthology$setGlobalBlockEntities(Set<BlockEntity> blockEntities);

    @Accessor("lastCameraSectionX")
    int anthology$getLastCameraSectionX();

    @Accessor("lastCameraSectionX")
    void anthology$setLastCameraSectionX(int value);

    @Accessor("lastCameraSectionY")
    int anthology$getLastCameraSectionY();

    @Accessor("lastCameraSectionY")
    void anthology$setLastCameraSectionY(int value);

    @Accessor("lastCameraSectionZ")
    int anthology$getLastCameraSectionZ();

    @Accessor("lastCameraSectionZ")
    void anthology$setLastCameraSectionZ(int value);

    @Accessor("prevCamX")
    double anthology$getPrevCamX();

    @Accessor("prevCamX")
    void anthology$setPrevCamX(double value);

    @Accessor("prevCamY")
    double anthology$getPrevCamY();

    @Accessor("prevCamY")
    void anthology$setPrevCamY(double value);

    @Accessor("prevCamZ")
    double anthology$getPrevCamZ();

    @Accessor("prevCamZ")
    void anthology$setPrevCamZ(double value);

    @Accessor("prevCamRotX")
    double anthology$getPrevCamRotX();

    @Accessor("prevCamRotX")
    void anthology$setPrevCamRotX(double value);

    @Accessor("prevCamRotY")
    double anthology$getPrevCamRotY();

    @Accessor("prevCamRotY")
    void anthology$setPrevCamRotY(double value);

    @Accessor("xTransparentOld")
    double anthology$getTransparentSortX();

    @Accessor("xTransparentOld")
    void anthology$setTransparentSortX(double value);

    @Accessor("yTransparentOld")
    double anthology$getTransparentSortY();

    @Accessor("yTransparentOld")
    void anthology$setTransparentSortY(double value);

    @Accessor("zTransparentOld")
    double anthology$getTransparentSortZ();

    @Accessor("zTransparentOld")
    void anthology$setTransparentSortZ(double value);

    @Accessor("cloudBuffer")
    VertexBuffer anthology$getCloudBuffer();

    @Accessor("cloudBuffer")
    void anthology$setCloudBuffer(VertexBuffer cloudBuffer);

    @Accessor("generateClouds")
    boolean anthology$getGenerateClouds();

    @Accessor("generateClouds")
    void anthology$setGenerateClouds(boolean generateClouds);

    @Accessor("prevCloudX")
    int anthology$getPrevCloudX();

    @Accessor("prevCloudX")
    void anthology$setPrevCloudX(int value);

    @Accessor("prevCloudY")
    int anthology$getPrevCloudY();

    @Accessor("prevCloudY")
    void anthology$setPrevCloudY(int value);

    @Accessor("prevCloudZ")
    int anthology$getPrevCloudZ();

    @Accessor("prevCloudZ")
    void anthology$setPrevCloudZ(int value);

    @Accessor("prevCloudColor")
    Vec3 anthology$getPrevCloudColor();

    @Accessor("prevCloudColor")
    void anthology$setPrevCloudColor(Vec3 value);

    @Accessor("prevCloudsType")
    CloudStatus anthology$getPrevCloudsType();

    @Accessor("prevCloudsType")
    void anthology$setPrevCloudsType(CloudStatus value);

    @Accessor("rainSoundTime")
    int anthology$getRainSoundTime();

    @Accessor("rainSoundTime")
    void anthology$setRainSoundTime(int value);
}
