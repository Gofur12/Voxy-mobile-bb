package me.cortex.voxy.client.iris;

import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

//1.20.1 version (Iris 1.7.x). Differences to the main source set:
// - SamplerHolder has no Supplier<GlSampler> overloads and GlSampler has no MIPPED_NEAREST_NEAREST constant,
//   so the plain addDynamicSampler(IntSupplier, String...) overload is used and the depth texture keeps its own sampling state
public class VoxySamplers {
    public static void addSamplers(IrisRenderingPipeline pipeline, SamplerHolder samplers) {
        var patchData = ((IGetVoxyPatchData)pipeline).voxy$getPatchData();
        if (patchData != null) {
            String[] opaqueNames = new String[]{"vxDepthTexOpaque"};
            String[] translucentNames = new String[]{"vxDepthTexTrans"};
            /*
            if (IrisShaderPatch.IMPERSONATE_DISTANT_HORIZONS) {
                opaqueNames = new String[]{"vxDepthTexOpaque", "dhDepthTex1"};
                translucentNames = new String[]{"vxDepthTexTrans", "dhDepthTex", "dhDepthTex0"};
            }*/

            //TODO replace ()->0 with the actual depth texture id
            samplers.addDynamicSampler(() -> {
                var pipeData = ((IGetIrisVoxyPipelineData)pipeline).voxy$getPipelineData();
                if (pipeData == null) {
                    return 0;
                }
                if (pipeData.thePipeline == null) {
                    return 0;
                }

                //In theory the first frame could be null
                var dt = pipeData.thePipeline.fb.getDepthTex();
                if (dt == null) {
                    return 0;
                }
                return dt.id;
            }, opaqueNames);

            samplers.addDynamicSampler(() -> {
                var pipeData = ((IGetIrisVoxyPipelineData)pipeline).voxy$getPipelineData();
                if (pipeData == null) {
                    return 0;
                }
                if (pipeData.thePipeline == null) {
                    return 0;
                }
                //In theory the first frame could be null
                var dt = pipeData.thePipeline.fbTranslucent.getDepthTex();
                if (dt == null) {
                    return 0;
                }
                return dt.id;
            }, translucentNames);
        }
    }
}
