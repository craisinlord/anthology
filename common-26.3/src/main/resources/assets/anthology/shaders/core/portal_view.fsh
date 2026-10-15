#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 0) noperspective in vec2 portalUv;

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(texture(Sampler0, portalUv).rgb, 1.0);
}
