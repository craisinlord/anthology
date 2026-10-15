#version 330

uniform sampler2D Sampler0;

noperspective in vec2 portalUv;
out vec4 fragColor;

void main() {
    fragColor = vec4(texture(Sampler0, portalUv).rgb, 1.0);
}
