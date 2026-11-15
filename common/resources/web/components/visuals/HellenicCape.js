import { createSimpleComponent } from '/components/SimpleModule.js'

// Keep in sync with HellenicCape.CAPES (Java). Previews: /images/capes/<id>.png (outer side of the cape).
const capes = [
    { id: 'hellenic', name: 'Hellenic', note: 'Default' },
    { id: 'souvlaki', name: 'SouvlakiSMP', note: 'Server' },
    { id: 'klanovalvides', name: 'Klanovalvides', note: 'Clan' },
    { id: 'acab', name: 'A.C.A.B', note: 'Black & white' }
];

export function createComponent(template) {
    const args = createSimpleComponent('/api/hellenic-cape', template);
    const baseData = args.data;
    args.data = function () {
        return Object.assign(baseData.call(this), { capes });
    };
    args.methods.wear = function (id) {
        this.config.cape = id;
        this.config.enabled = true;
        this.update();
    };
    args.methods.isWorn = function (id) {
        if (!this.config.enabled) {
            return false;
        }
        const known = capes.some(c => c.id === this.config.cape);
        return known ? this.config.cape === id : id === capes[0].id;
    };
    return args;
}
