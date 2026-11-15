package me.cortex.voxy.client.core.model.bakery;


import com.mojang.blaze3d.vertex.VertexConsumer;
import me.cortex.voxy.common.util.MemoryBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.lwjgl.system.MemoryUtil;

//1.20.1 version. Differences to the main (26.x) source set:
// - VertexConsumer is the old vertex()/color()/uv()/endVertex() interface
// - BakedQuad has no materialInfo(), so the render layer and the sprite mipmap behaviour are supplied by the caller
// - quad vertex data is read from BakedQuad.getVertices() (DefaultVertexFormat.BLOCK, 8 ints per vertex)
public final class ReuseVertexConsumer implements VertexConsumer {
    public static final int VERTEX_FORMAT_SIZE = 24;
    private static final int QUAD_INTS_PER_VERTEX = 8;//position(3), colour, uv(2), light, normal
    private MemoryBuffer buffer = new MemoryBuffer(8192);
    private long ptr;
    private int count;
    private int defaultMeta;

    public boolean anyShaded;
    public boolean anyDarkendTex;
    public boolean anyDiscard;

    private final int globalOrMetadata;
    public ReuseVertexConsumer() {
        this(0);
    }
    public ReuseVertexConsumer(int globalOrMetadata) {
        this.reset();
        this.globalOrMetadata = globalOrMetadata;
    }

    public ReuseVertexConsumer setDefaultMeta(int meta) {
        this.defaultMeta = meta;
        return this;
    }

    public int getDefaultMeta() {
        return this.defaultMeta;
    }

    @Override
    public ReuseVertexConsumer vertex(double x, double y, double z) {
        this.ensureCanPut();
        this.ptr += VERTEX_FORMAT_SIZE; this.count++; //Goto next vertex
        this.meta(this.defaultMeta|this.globalOrMetadata);
        MemoryUtil.memPutFloat(this.ptr, (float) x);
        MemoryUtil.memPutFloat(this.ptr + 4, (float) y);
        MemoryUtil.memPutFloat(this.ptr + 8, (float) z);
        return this;
    }

    public ReuseVertexConsumer meta(int metadata) {
        this.anyDiscard |= (metadata&1)!=0;
        MemoryUtil.memPutInt(this.ptr + 12, metadata);
        return this;
    }

    @Override
    public ReuseVertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public ReuseVertexConsumer uv(float u, float v) {
        MemoryUtil.memPutFloat(this.ptr + 16, u);
        MemoryUtil.memPutFloat(this.ptr + 20, v);
        return this;
    }

    @Override
    public ReuseVertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Override
    public ReuseVertexConsumer uv2(int u, int v) {
        return this;
    }

    @Override
    public ReuseVertexConsumer normal(float x, float y, float z) {
        return this;
    }

    @Override
    public void endVertex() {
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
    }

    @Override
    public void unsetDefaultColor() {
    }

    public ReuseVertexConsumer quad(BakedQuad quad) {
        return this.quad(quad, false, false);
    }

    //hasDiscard: the quad is rendered in a layer with alpha discard (cutout/translucent), 1.20.1 quads do not carry their layer
    //darkenedTexture: the sprite of the quad is a cutout texture whose mips get darkened, 1.20.1 sprites do not carry this either
    public ReuseVertexConsumer quad(BakedQuad quad, boolean hasDiscard, boolean darkenedTexture) {
        int meta = 0;
        meta |= hasDiscard?1:0;//has discard
        meta |= quad.isTinted()?4:0;//has tinting
        this.anyDarkendTex |= darkenedTexture;
        return this.quad(quad, meta);
    }

    public ReuseVertexConsumer quad(BakedQuad quad, int metadata) {
        //TODO: FIXME: this is wrong, e.g. stuff becomes conditionally shaded, e.g. the shade comes from the override pretty sure
        this.anyShaded |= quad.isShade();
        this.ensureCanPut();
        int[] data = quad.getVertices();
        for (int i = 0; i < 4; i++) {
            int base = i * QUAD_INTS_PER_VERTEX;
            this.vertex(Float.intBitsToFloat(data[base]), Float.intBitsToFloat(data[base + 1]), Float.intBitsToFloat(data[base + 2]));
            this.uv(Float.intBitsToFloat(data[base + 4]), Float.intBitsToFloat(data[base + 5]));

            this.meta(metadata|this.globalOrMetadata);
        }
        return this;
    }

    private void ensureCanPut() {
        if ((long) (this.count + 5) * VERTEX_FORMAT_SIZE < this.buffer.size) {
            return;
        }
        long offset = this.ptr-this.buffer.address;
        //1.5x the size
        var newBuffer = new MemoryBuffer((((int)(this.buffer.size*2)+VERTEX_FORMAT_SIZE-1)/VERTEX_FORMAT_SIZE)*VERTEX_FORMAT_SIZE);
        this.buffer.cpyTo(newBuffer.address);
        this.buffer.free();
        this.buffer = newBuffer;
        this.ptr = offset + newBuffer.address;
    }

    public ReuseVertexConsumer reset() {
        this.anyShaded = false;
        this.anyDarkendTex = false;
        this.anyDiscard = false;
        this.defaultMeta = 0;//RESET THE DEFAULT META
        this.count = 0;
        this.ptr = this.buffer.address - VERTEX_FORMAT_SIZE;//the thing is first time this gets incremented by FORMAT_STRIDE
        return this;
    }

    public void free() {
        this.ptr = 0;
        this.count = 0;
        this.buffer.free();
        this.buffer = null;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public int quadCount() {
        if (this.count%4 != 0) throw new IllegalStateException();
        return this.count/4;
    }

    public long getAddress() {
        return this.buffer.address;
    }
}
